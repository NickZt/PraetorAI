import time
import os
import shutil
from neo4j import GraphDatabase

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"

def verify_audio_ingestion():
    # 1. Prepare Paths
    ingest_dir = "/home/nickzt/Projects/TactOrder/RDSS/data/ingest"
    test_audio = "/home/nickzt/Projects/TactOrder/RDSS/test_suite/test_audio.wav"
    target_path = os.path.join(ingest_dir, "test_audio.wav")
    
    if not os.path.exists(ingest_dir):
        os.makedirs(ingest_dir)
        
    print(f"🎙️ Copying test audio to {target_path}...")
    shutil.copy(test_audio, target_path)
    
    # 2. Wait for processing (Inference takes time)
    print("⏳ Waiting 60s for AcousticAgent to process audio...")
    time.sleep(60)
    
    # 3. Verify Neo4j
    print("🔍 Querying Neo4j for Audio node...")
    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
    with driver.session() as session:
        result = session.run("MATCH (a:Audio) WHERE a.name = 'test_audio.wav' RETURN a.transcript as transcript")
        record = result.single()
        
        if record:
            print(f"✅ FOUND Audio Node!")
            print(f"🗣️ Transcript: {record['transcript']}")
            assert record['transcript'] is not None
        else:
            print("❌ Audio Node NOT found in Neo4j.")
            exit(1)
            
    driver.close()

if __name__ == "__main__":
    try:
        verify_audio_ingestion()
        print("\n✨ Audio Ingestion Verification SUCCESSFUL!")
    except Exception as e:
        print(f"\n❌ Audio Ingestion Verification FAILED: {e}")
        exit(1)
