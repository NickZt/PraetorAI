import time
import os
import shutil
from neo4j import GraphDatabase

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "password"

def verify_vision_ingestion():
    # 1. Prepare Paths
    ingest_dir = "/home/nickzt/Projects/TactOrder/RDSS/data/ingest"
    test_image = "/home/nickzt/Projects/TactOrder/RDSS/test_suite/test_vision.png"
    target_path = os.path.join(ingest_dir, "test_vision.png")
    
    if not os.path.exists(ingest_dir):
        os.makedirs(ingest_dir)
        
    print(f"📸 Copying test image to {target_path}...")
    shutil.copy(test_image, target_path)
    
    # 2. Wait for processing (MNN inference takes time)
    print("⏳ Waiting 120s for VisionAgent to process image...")
    time.sleep(120)
    
    # 3. Verify Neo4j
    print("🔍 Querying Neo4j for Image node...")
    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
    with driver.session() as session:
        result = session.run("MATCH (i:Image) WHERE i.name = 'test_vision.png' RETURN i.description as desc, i.ocrText as ocr")
        record = result.single()
        
        if record:
            print(f"✅ FOUND Image Node!")
            print(f"📝 Description: {record['desc']}")
            print(f"🔠 OCR Text: {record['ocr']}")
            assert record['desc'] is not None
        else:
            print("❌ Image Node NOT found in Neo4j.")
            exit(1)
            
    driver.close()

if __name__ == "__main__":
    try:
        verify_vision_ingestion()
        print("\n✨ Vision Ingestion Verification SUCCESSFUL!")
    except Exception as e:
        print(f"\n❌ Vision Ingestion Verification FAILED: {e}")
        exit(1)
