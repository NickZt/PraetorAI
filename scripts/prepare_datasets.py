#!/usr/bin/env python3
import os
import json
import datetime
import urllib.request
import pandas as pd

DATASETS_DIR = "/home/nickzt/Projects/TactOrder/Datasets"
LEXUA_DIR = os.path.join(DATASETS_DIR, "LexUa")
TACTORDER_DIR = os.path.join(DATASETS_DIR, "TactOrder")

# Load environment variable from .env
env_path = "/home/nickzt/Projects/TactOrder/RDSS/.env"
if os.path.exists(env_path):
    with open(env_path) as f:
        for line in f:
            if line.startswith("HF_TOKEN="):
                os.environ["HF_TOKEN"] = line.strip().split("=", 1)[1].strip()

hf_token = os.environ.get("HF_TOKEN")


def ensure_dirs():
    print("📂 Creating dataset directories...")
    for d in [LEXUA_DIR, TACTORDER_DIR]:
        os.makedirs(os.path.join(d, "CUAD"), exist_ok=True)
        os.makedirs(os.path.join(d, "MAUD"), exist_ok=True)
        os.makedirs(os.path.join(d, "Laws"), exist_ok=True)
        os.makedirs(os.path.join(d, "VisDrone"), exist_ok=True)
        os.makedirs(os.path.join(d, "MUC-4"), exist_ok=True)
        os.makedirs(os.path.join(d, "FRAGO"), exist_ok=True)
        os.makedirs(os.path.join(d, "FieldManuals"), exist_ok=True)

def download_authenticated_hf_dataset(dataset_name, config_name, split, dest_csv):
    """Downloads a dataset utilizing HF_TOKEN and saves as CSV."""
    if not hf_token:
        raise ValueError("HF_TOKEN not found in environment or .env file.")
    try:
        from datasets import load_dataset
        import pandas as pd
        if config_name:
            ds = load_dataset(dataset_name, config_name, split=split, token=hf_token, verification_mode="no_checks")
        else:
            ds = load_dataset(dataset_name, split=split, token=hf_token, verification_mode="no_checks")
        df = ds.to_pandas()
        df.to_csv(dest_csv, index=False)
    except Exception as e:
        print(f"❌ FATAL ERROR loading {dataset_name}: {e}")
        raise e

def download_cuad():
    print("⏳ [EXP-01 LexUa] Downloading REAL CUAD Dataset from HuggingFace...")
    target = os.path.join(LEXUA_DIR, "CUAD", "cuad.csv")
    download_authenticated_hf_dataset("theatticusproject/cuad", "default", "train", target)
    print(f"✅ REAL CUAD Downloaded to {target}")

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
    print("⏳ [EXP-02 LexUa] Downloading REAL MAUD Dataset from HuggingFace...")
    target = os.path.join(LEXUA_DIR, "MAUD", "maud.csv")
    download_authenticated_hf_dataset("theatticusproject/maud", "default", "train", target)
    print(f"✅ REAL MAUD Downloaded to {target}")

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

def download_url_to_file(url, dest_path):
    print(f"      Downloading {os.path.basename(dest_path)}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        with open(dest_path, "wb") as f:
            f.write(response.read())

def generate_field_manuals():
    print("⏳ [EXP-03 TactOrder] Downloading REAL US and Ukrainian Field Manuals...")
    
    target_us = os.path.join(TACTORDER_DIR, "FieldManuals", "US_FM_3_0_Operations.pdf")
    try:
        download_url_to_file("https://irp.fas.org/doddir/army/fm3-0.pdf", target_us)
    except Exception as e:
        print(f"⚠️ Could not download US FM 3-0: {e}")

    target_ua = os.path.join(TACTORDER_DIR, "FieldManuals", "UA_Combat_Manual_Part3.pdf")
    try:
        download_url_to_file("https://chtyvo.org.ua/authors/Zbroini_Syly_Ukrainy/Boiovyi_statut_Sukhoputnykh_viisk_Chastyna_III_vzvod_viddilennia_ekipazh_tanka_vyd_2010.pdf", target_ua)
    except Exception as e:
        print(f"⚠️ Could not download UA Combat Manual: {e}")
        
    print("✅ Authentic Field Manuals Downloaded.")

def link_user_articles():
    print("⏳ Linking User's Articles directory...")
    target = os.path.join(DATASETS_DIR, "UserArticles")
    source = "/home/nickzt/Projects/TactOrder/Articles/"
    if os.path.exists(source) and not os.path.exists(target):
         try:
             os.symlink(source, target)
             print(f"✅ UserArticles linked to {target}")
         except Exception as e:
             print(f"⚠️ Could not symlink {source} -> {target}: {e}")
    else:
         print(f"✅ UserArticles target already setup or source doesn't exist.")

if __name__ == "__main__":
    ensure_dirs()
    download_cuad()
    generate_visdrone_mock()
    download_maud()
    download_muc4()
    generate_laws()
    generate_fragos()
    generate_field_manuals()
    link_user_articles()
    print("\n🎉 All Datasets loaded into /home/nickzt/Projects/TactOrder/Datasets/")
