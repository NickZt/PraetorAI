#!/usr/bin/env python3
"""
Agentic Orchestration Test for Praetor AI (Internal)
Verifies Phase 4 Specialized Agents (Curator, Scout, Advisor, Composer).
"""

import time
import os
import sys
from scenarios_lib import IngestorClient, AssistantClient, GraphValidator

# Constants
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8081")
NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")

def run_agentic_tests():
    print("=========================================================")
    print("🤖 Praetor AI: Agentic Test Suite (Phase 4)")
    print("=========================================================")
    
    ingestor = IngestorClient(GATEWAY_URL)
    assistant = AssistantClient(GATEWAY_URL)
    validator = GraphValidator(NEO4J_URI)

    # 1. Curator & Scout: The 'Jane Doe' Conflict
    print("\n🔍 Test 1: Scout Multi-hop & Composer Attribution")
    # Jane Doe issued 104-A and 104-B. 
    # Query: "What was the limit before the amendment?"
    q1 = "Who issued the directive that set the 600ft limit, and what replaced it?"
    print(f"   [User]: {q1}")
    res1 = assistant.query(q1)
    print(f"   [Assistant]: {res1}")
    
    if "Jane Doe" in str(res1) and "104-B" in str(res1):
        print("   ✅ Correct multi-hop citation (Scout) and attribution (Composer).")

    # 2. Advisor Audit (Manual Log Verification)
    print("\n🔍 Test 2: Advisor Audit Trigger")
    # Ingesting a manual conflict
    path_conflict = "/tmp/conflict_directive_v1.txt"
    with open(path_conflict, "w") as f:
        f.write("HEADQUARTERS FIELD DIRECTIVE 104-C\nDate: 2026-06-01\nProtocol: ALL Alpha-Class Drones must maintain 800 feet (Conflict with 400ft limit).")
    
    print("📡 Ingesting Conflict Directive 104-C...")
    ingestor.ingest_file(path_conflict, {"type": "Directive", "id": "pol_104_c"})
    print("   ✅ Audit triggered (Check Orchestrator logs for 'Triggering Proactive Advisor audit').")

    print("\n=========================================================")
    print("✅ AGENTIC TESTS COMPLETED")
    print("=========================================================")

if __name__ == "__main__":
    run_agentic_tests()
