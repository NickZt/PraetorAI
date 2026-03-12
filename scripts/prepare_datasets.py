#!/usr/bin/env python3
"""
Dataset Preparation Script for Praetor AI Experiments
Target Directory: /home/nickzt/Projects/TactOrder/Datasets/

Downloads and/or generates the necessary datasets for E2E Validation:
- EXP-01: CUAD (LexUa) & VisDrone Mock (TactOrder)
- EXP-02: MAUD (LexUa) & MUC-4 Mock (TactOrder)
- EXP-03: Temporal Laws (LexUa) & OPORD/FRAGO Logs (TactOrder)
"""

import os
import json
import uuid
import datetime

DATASETS_DIR = "/home/nickzt/Projects/TactOrder/Datasets"
LEXUA_DIR = os.path.join(DATASETS_DIR, "LexUa")
TACTORDER_DIR = os.path.join(DATASETS_DIR, "TactOrder")

def ensure_dirs():
    print("📂 Creating dataset directories...")
    for d in [LEXUA_DIR, TACTORDER_DIR]:
        os.makedirs(os.path.join(d, "CUAD"), exist_ok=True)
        os.makedirs(os.path.join(d, "MAUD"), exist_ok=True)
        os.makedirs(os.path.join(d, "Laws"), exist_ok=True)
        os.makedirs(os.path.join(d, "VisDrone"), exist_ok=True)
        os.makedirs(os.path.join(d, "MUC-4"), exist_ok=True)
        os.makedirs(os.path.join(d, "FRAGO"), exist_ok=True)

def download_cuad():
    print("⏳ [EXP-01 LexUa] Downloading CUAD dataset...")
    target = os.path.join(LEXUA_DIR, "CUAD", "cuad_chunks.jsonl")
    try:
        from datasets import load_dataset
        ds = load_dataset("Zane222/cuad", split="train")
        # Just grab first 50 texts and chunk them
        with open(target, "w") as f:
            for item in list(ds)[:50]:
                text = item.get("context", "")
                chunk = text[:1500] # Approximate 512 tokens
                f.write(json.dumps({"text": chunk}) + "\n")
        print("✅ CUAD Downloaded via HuggingFace.")
    except Exception as e:
        print(f"⚠️ HuggingFace load failed ({e}), generating MOCK CUAD text...")
        with open(target, "w") as f:
            for i in range(50):
                f.write(json.dumps({"text": f"This is mock contract chunk {i} representing a 512 token payload for civil testing."}) + "\n")

def generate_visdrone_mock():
    print("⏳ [EXP-01 TactOrder] Generating VisDrone Telemetry Mock...")
    target = os.path.join(TACTORDER_DIR, "VisDrone", "telemetry.json")
    frames = []
    base_time = datetime.datetime.now()
    for i in range(100):
        frames.append({
            "frame_id": i,
            "timestamp": (base_time + datetime.timedelta(milliseconds=i*33)).isoformat(),
            "bbox": [100+i, 200, 50, 50],
            "class": "vehicle"
        })
    with open(target, "w") as f:
        json.dump(frames, f, indent=2)
    print("✅ VisDrone Mock Generated.")

def download_maud():
    print("⏳ [EXP-02 LexUa] Downloading MAUD dataset...")
    target = os.path.join(LEXUA_DIR, "MAUD", "maud_texts.jsonl")
    try:
        from datasets import load_dataset
        ds = load_dataset("ds_maud", "default", split="train")
        with open(target, "w") as f:
            for item in list(ds)[:50]:
                f.write(json.dumps({"text": item.get("text", "")}) + "\n")
        print("✅ MAUD Downloaded.")
    except Exception as e:
        print(f"⚠️ MAUD load failed ({e}), generating MOCK MAUD text...")
        with open(target, "w") as f:
            for i in range(50):
                f.write(json.dumps({"text": f"Mock Merger Agreement {i} for NER scaling."}) + "\n")

def download_muc4():
    print("⏳ [EXP-02 TactOrder] MUC-4 Incident Mock...")
    target = os.path.join(TACTORDER_DIR, "MUC-4", "incidents.jsonl")
    with open(target, "w") as f:
        for i in range(50):
            f.write(json.dumps({"text": f"Incident report {i}: Unidentified vehicle approaching sector 7. Requesting reinforcement."}) + "\n")
    print("✅ MUC-4 Mock Generated.")

def generate_laws():
    print("⏳ [EXP-03 LexUa] Generating Civil Code Markdown with timelines...")
    target = os.path.join(LEXUA_DIR, "Laws", "civil_code_article_400.md")
    content = """# Цивільний Кодекс: Стаття 400
**Effective Date:** 2020-01-01
Орган влади зобов'язаний проводити аудит раз на 3 роки.

# Поправка до Статті 400
**Effective Date:** 2023-01-01
Орган влади зобов'язаний проводити аудит раз на 1 рік. Обов'язкове залучення незалежних експертів.
"""
    with open(target, "w") as f:
        f.write(content)
    print("✅ Laws Generated.")

def generate_fragos():
    print("⏳ [EXP-03 TactOrder] Generating OPORD/FRAGO Logs...")
    target = os.path.join(TACTORDER_DIR, "FRAGO", "combat_log.json")
    logs = {
        "OPORD": {
            "id": "OP-992",
            "date": "2024-05-01T06:00:00Z",
            "objective": "Hold Phase Line Alpha",
            "status": "ISSUED"
        },
        "FRAGOs": [
            {
                "id": "FR-01",
                "date": "2024-05-02T12:00:00Z",
                "objective": "Fallback to Phase Line Bravo due to heavy resistance",
                "status": "ISSUED"
            },
            {
                "id": "FR-02",
                "date": "2024-05-03T08:00:00Z",
                "objective": "Hold Phase Line Bravo and await resupply",
                "status": "ISSUED"
            }
        ]
    }
    with open(target, "w") as f:
        json.dump(logs, f, indent=2)
    print("✅ TactOrder OPORD/FRAGO Generated.")

if __name__ == "__main__":
    ensure_dirs()
    download_cuad()
    generate_visdrone_mock()
    download_maud()
    download_muc4()
    generate_laws()
    generate_fragos()
    print("\n🎉 All Datasets loaded into /home/nickzt/Projects/TactOrder/Datasets/")
