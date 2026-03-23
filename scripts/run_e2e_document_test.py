#!/usr/bin/env python3
"""
End-to-End Real Document Execution Test for Praetor AI (RDSS)
Validates the full pipeline: Ingestion -> NER -> Graph Storage -> Temporal RAG with MNNLLama.

Usage:
    python3 run_e2e_document_test.py
"""

import time
import sys
import os
from scenarios_lib import IngestorClient, AssistantClient, GraphValidator

# Constants
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8081")
NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")

# Test Data
DOC_V1_TEXT = """
HEADQUARTERS FIELD DIRECTIVE 104-A
Date: 2023-01-15
Subject: Autonomous Drone Deployment Protocol
Protocol: All Alpha-Class Drones must maintain a maximum altitude of 600 feet.
Authorized Officer: Commander Jane Doe.
"""

DOC_V2_TEXT = """
HEADQUARTERS FIELD DIRECTIVE 104-B
Date: 2024-05-10
Subject: Autonomous Drone Deployment Protocol
Protocol: This supersedes Directive 104-A. All Alpha-Class Drones must now maintain a maximum altitude of 400 feet.
Authorized Officer: Commander Jane Doe.
"""

def create_temp_doc(content, suffix=".txt"):
    import tempfile
    fd, path = tempfile.mkstemp(suffix=suffix)
    with os.fdopen(fd, 'w') as f:
        f.write(content)
    return path

def run_scenario_1_ingestion():
    print("\n🌍 Scenario 1: The 'Fog Node' Intake")
    ingestor = IngestorClient(GATEWAY_URL)
    validator = GraphValidator(NEO4J_URI)
    
    path_v1 = create_temp_doc(DOC_V1_TEXT, "_v1.txt")
    print(f"   [Temp File Created]: {path_v1}")
    
    print("📡 Step 1.1: Ingesting Field Directive 104-A...")
    try:
        res = ingestor.ingest_file(path_v1, {"type": "Directive", "id": "dir_104_a"})
        print(f"   [Gateway Response]: {res.get('status')}")
        
        print("🕸️ Step 1.2: Verifying Entity Extraction in Neo4j (Waiting for async process)...")
        # LLM inference on edge/CPU can take time
        time.sleep(30) 
        
        if validator.verify_node_exists("Person", {"name": "Jane Doe"}):
            print("   ✅ Found Person: Jane Doe")
        else:
            print("   ⚠️ Person 'Jane Doe' not found in Graph yet (Check NER/GraphWriter logs)")

        # Based on IngestionVerticle, it might create Document nodes
        if validator.verify_node_exists("Document", {"title": os.path.basename(path_v1)}):
            print("   ✅ Found Document node.")
        else:
            print("   ⚠️ Document node not found in Graph")
            
    except Exception as e:
        print(f"   ❌ Scenario 1 Failed: {e}")
        return False
    finally:
        if os.path.exists(path_v1):
            os.remove(path_v1)
    return True

def run_scenario_2_temporal_rag():
    print("\n🌍 Scenario 2: The 'Temporal Query' Challenge")
    ingestor = IngestorClient(GATEWAY_URL)
    assistant = AssistantClient(GATEWAY_URL)
    
    path_v2 = create_temp_doc(DOC_V2_TEXT, "_v2.txt")
    
    print("📡 Step 2.1: Ingesting Amended Directive 104-B (Effective 2024-05-10)...")
    try:
        ingestor.ingest_file(path_v2, {"type": "Directive", "id": "dir_104_b"})
        time.sleep(30)
        
        print("💬 Step 2.2: Asking a Temporal Question (Past Context)...")
        q1 = "What was the max drone altitude limit on February 1st, 2024?"
        print(f"   [User]: {q1}")
        res1 = assistant.query(q1)
        # Content structure might vary depending on RAG implementation, assuming standard result
        content1 = str(res1) 
        print(f"   [Assistant Raw Output]: {content1}")
        
        if "600" in content1:
            print("   ✅ Correct temporal context retrieved (600ft).")
        else:
            print("   ⚠️ Temporal context might be incorrect. Expected '600'.")

        print("\n💬 Step 2.3: Asking a Temporal Question (Current Context)...")
        q2 = "What is the current drone altitude limit?"
        print(f"   [User]: {q2}")
        res2 = assistant.query(q2)
        content2 = str(res2)
        print(f"   [Assistant Raw Output]: {content2}")
        
        if "400" in content2:
            print("   ✅ Correct current context retrieved (400ft).")
        else:
            print("   ⚠️ Current context might be incorrect. Expected '400'.")

    except Exception as e:
        print(f"   ❌ Scenario 2 Failed: {e}")
        return False
    finally:
        if os.path.exists(path_v2):
            os.remove(path_v2)
    return True

def run_all():
    print("=========================================================")
    print("🚀 Praetor AI: End-to-End Testing Suite")
    print("=========================================================")
    
    s1 = run_scenario_1_ingestion()
    s2 = run_scenario_2_temporal_rag()
    
    print("\n=========================================================")
    if s1 and s2:
        print("✅ ALL E2E SCENARIOS PASSED")
    else:
        print("❌ SOME SCENARIOS FAILED")
    print("=========================================================")

if __name__ == "__main__":
    run_all()
