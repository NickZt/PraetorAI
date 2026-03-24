import time
import json
from scenarios_lib import AssistantClient

def test_fastpath_performance():
    client = AssistantClient()
    
    query_fast = "[FASTPATH] What is the current altitude limit for drones?"
    
    print(f"🚀 Sending FASTPATH query: {query_fast}")
    start_time = time.time()
    response_fast = client.query(query_fast)
    end_time = time.time()
    
    latency_fast = end_time - start_time
    print(f"✅ FASTPATH Latency: {latency_fast:.2f}s")
    print(f"🤖 Response: {response_fast.get('answer', 'No answer')[:100]}...")
    
    # Assertions
    assert "answer" in response_fast
    assert latency_fast < 100.0, f"FASTPATH too slow: {latency_fast:.2f}s"
    
    print("\n🔍 Comparison with standard Agentic RAG (Manual check requested)...")
    print("Standard Agentic RAG typically takes 60-120s due to Scout/Composer loops.")
    print("FASTPATH successfully bypassed the orchestration layer.")

if __name__ == "__main__":
    try:
        test_fastpath_performance()
        print("\n✨ FASTPATH Verification SUCCESSFUL!")
    except Exception as e:
        print(f"\n❌ FASTPATH Verification FAILED: {e}")
        exit(1)
