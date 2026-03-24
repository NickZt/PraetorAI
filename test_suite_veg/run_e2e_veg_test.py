#!/usr/bin/env python3
"""
Vegetarian (Sanitized) E2E Test for Praetor AI (Open Source)
Uses generic data for public testing.
"""

import time
import os
from scenarios_lib import IngestorClient, AssistantClient, GraphValidator

# Constants
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8081")
NEO4J_URI = os.getenv("NEO4J_URI", "bolt://localhost:7687")

# Sanitized Test Data (VEG)
DOC_V1_TEXT = """
PROJECT POLICY 101-A
Date: 2025-01-15
Subject: General Data Access
Policy: All Junior Analysts must have a maximum data limit of 10GB.
Authorized Officer: Alice Smith.
"""

DOC_V2_TEXT = """
PROJECT POLICY 101-B
Date: 2026-05-10
Subject: General Data Access
Policy: This supersedes Policy 101-A. All Junior Analysts must now have a maximum data limit of 5GB.
Authorized Officer: Alice Smith.
"""

def create_temp_doc(content, name):
    path = os.path.join("data", "ingest_veg", name)
    with open(path, "w") as f:
        f.write(content)
    return path

def run_all():
    print("=========================================================")
    print("🥬 Praetor AI: Vegetarian E2E Test Suite (Sanitized)")
    print("=========================================================")
    
    ingestor = IngestorClient(GATEWAY_URL)
    validator = GraphValidator(NEO4J_URI)
    assistant = AssistantClient(GATEWAY_URL)

    # Step 1: Ingest V1
    path_v1 = create_temp_doc(DOC_V1_TEXT, "pol_101_a_veg.txt")
    print(f"📡 Step 1: Ingesting Policy 101-A into {path_v1}...")
    # No need to call ingestor.ingest_file because FileWatcher will pick it up
    time.sleep(30)
    
    if validator.verify_node_exists("Person", {"name": "Alice Smith"}):
        print("   ✅ Found Person: Alice Smith")
    
    # Step 2: Ingest V2 (Supersedes)
    path_v2 = create_temp_doc(DOC_V2_TEXT, "pol_101_b_veg.txt")
    print(f"📡 Step 2: Ingesting Policy 101-B into {path_v2}...")
    time.sleep(30)
    
    # Step 3: Temporal Query
    print("💬 Step 3: Asking about past limit (2025)...")
    res1 = assistant.query("What was the data limit in 2025?")
    print(f"   [Assistant]: {res1}")
    
    if "10" in str(res1):
        print("   ✅ Correct temporal context (10GB).")

    print("\n=========================================================")
    print("✅ VEGETARIAN TEST COMPLETED")
    print("=========================================================")

if __name__ == "__main__":
    run_all()
