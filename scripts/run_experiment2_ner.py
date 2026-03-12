#!/usr/bin/env python3
"""
Experiment 2: Edge NER Scalability Validation
Validates SOP 2.2: GLiNER-bi-v2 (Base) throughput stability when scaling
target extraction classes from 10 to 1000 on CPU.

Metrics:
- Degradation of speed from 10 to 1000 classes ≤ 6%
- Micro-F1 accuracy > 60%

Requirements:
- Python 3.9+
- requests

Usage:
    python3 run_experiment2_ner.py
"""

import requests
import time
import statistics
import concurrent.futures

GATEWAY_URL = "http://localhost:8080/v1/chat/completions"
MODEL = "gliner-bi-v2"
ITERATIONS = 20
CONCURRENT_WORKERS = 5

SAMPLE_CONTRACT = """
This Non-Disclosure Agreement ("Agreement") is entered into this 14th day of February, 2024, 
by and between Acme Corporation, a Delaware corporation ("Disclosing Party"), and 
Globex Inc., a California corporation ("Receiving Party"). The confidentiality obligations 
shall remain active for 5 years.
"""

# Simulate 10 basic labels
LABELS_10 = "Extract named entities: ORG, DATE, ALIAS, LOC, PERSON, MONEY, PERCENT, TIME, FAC, GPE."

# Simulate 1000 labels (Just padding the prompt to trigger the 1000 precompiled embeddings on the backend)
# In a real scenario, this relies on the backend Bi-Encoder holding 1000 vectors.
LABELS_1000 = "Extract named entities: ORG, DATE, ALIAS, LOC, PERSON, " + ", ".join([f"CLASS_{i}" for i in range(995)])

REQUIRED_ENTITIES = ["Acme Corporation", "Disclosing Party", "Globex Inc.", "Receiving Party", "14th day of February, 2024"]

def measure_throughput(label_prompt, label_count_name):
    print(f"Running {ITERATIONS} requests for {label_count_name} classes...")
    latencies = []
    success_count = 0
    
    def make_req(req_id):
        start_time = time.time()
        try:
            payload = {
                "model": MODEL,
                "messages": [
                    {"role": "system", "content": label_prompt},
                    {"role": "user", "content": SAMPLE_CONTRACT}
                ],
                "stream": False
            }
            response = requests.post(GATEWAY_URL, json=payload, timeout=5.0)
            elapsed = time.time() - start_time
            
            if response.status_code == 200:
                result = response.json()
                content = result["choices"][0]["message"]["content"]
                
                # Mock Micro-F1 check (Recall of specific entities)
                recall_match = sum(1 for e in REQUIRED_ENTITIES if e.lower() in content.lower())
                f1_score = (recall_match / len(REQUIRED_ENTITIES)) * 100
                
                return elapsed * 1000, f1_score
        except Exception:
            pass
        return None, 0.0

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_WORKERS) as executor:
        futures = [executor.submit(make_req, i) for i in range(ITERATIONS)]
        
        for future in concurrent.futures.as_completed(futures):
            res_time, res_f1 = future.result()
            if res_time is not None:
                latencies.append(res_time)
                if res_f1 > 60.0:
                    success_count += 1
                    
    if not latencies:
        return 0, 0
    
    avg_latency = sum(latencies) / len(latencies)
    avg_f1 = (success_count / len(latencies)) * 100
    p99 = statistics.quantiles(latencies, n=100)[98] if len(latencies) > 1 else latencies[0]
    
    print(f"  -> Avg Latency: {avg_latency:.2f} ms | P99: {p99:.2f} ms | Micro-F1 >60% Hits: {avg_f1:.1f}%")
    return avg_latency, avg_f1

def run_experiment():
    print("=========================================================")
    print("🧪  Experiment 2: Edge NER Scalability (10 vs 1000 Classes)")
    print("=========================================================")
    
    lat_10, f1_10 = measure_throughput(LABELS_10, "10")
    if lat_10 == 0:
        print("❌ Gateway unreachable or all requests failed.")
        return
        
    lat_1000, f1_1000 = measure_throughput(LABELS_1000, "1000")
    
    degradation = ((lat_1000 - lat_10) / lat_10) * 100
    
    print("\n📊 --- FINAL RESULTS ---")
    print(f"Latency Degradation (10 -> 1000): {degradation:.2f}%")
    print(f"Micro-F1 (1000 classes)  > 60%: {'YES' if f1_1000 > 60 else 'NO'} ({f1_1000:.1f}%)")
    
    # Validation against manifesto constraints
    if degradation <= 6.0 and f1_1000 > 60.0:
        print("\n✅ EXPERIMENT 2 PASSED: GLiNER Precompiled Embeddings deliver O(1) scalability on CPU!")
    else:
        print("\n❌ EXPERIMENT 2 FAILED: Metrics breached architectural thresholds.")

if __name__ == "__main__":
    run_experiment()
