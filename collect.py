#!/usr/bin/env python3
"""
collect.py 扔 CreditCardManager/ 下跑：
  cd CreditCardManager
  python collect.py
→ 生成 local/ 扁平 .txt，全选拖对话发 AI

文件名规则：
  原: app/src/main/java/com/creditcardmanager/utils/ExportImportManager.kt
  编: com_creditcardmanager_utils__ExportImportManager.kt.txt
  AI 见到 __ 劈，前半 _→/ 还原包路径，拼回 app/src/main/java/...
"""

from pathlib import Path

PROJECT_ROOT = Path(".")          # ← 扔 CreditCardManager/ 下，"."
DEST = Path("./local")

FILES = [
    # 闪退 + 空白
    "app/src/main/java/com/creditcardmanager/utils/ExportImportManager.kt",
    "app/src/main/java/com/creditcardmanager/model/ExportData.kt",
    "app/src/main/java/com/creditcardmanager/data/local/Converters.kt",
    "app/src/main/java/com/creditcardmanager/data/local/entity/ActivityProgressEntity.kt",
    "app/src/main/java/com/creditcardmanager/utils/ActivityCalculator.kt",
    # 重算链 + 手动调整 VM
    "app/src/main/java/com/creditcardmanager/viewmodel/TransactionViewModel.kt",
    "app/src/main/java/com/creditcardmanager/viewmodel/ActivityViewModel.kt",
    "app/src/main/java/com/creditcardmanager/viewmodel/SettingsViewModel.kt",
    # 手动调整 UI
    "app/src/main/java/com/creditcardmanager/ui/activities/ActivityDetailFragment.kt",
    # enum 大小写嫌疑
    "app/src/main/java/com/creditcardmanager/model/Activity.kt",
    "app/src/main/java/com/creditcardmanager/model/ActivityReward.kt",
    "app/src/main/java/com/creditcardmanager/model/enums/InnerType.kt",
]

def encode_name(rel: str) -> str:
    parts = rel.split("/")
    if "java" in parts:
        idx = parts.index("java") + 1
        pkg_part = "_".join(parts[idx:-1])
        base = parts[-1]
    elif "res" in parts:
        idx = parts.index("res") + 1
        pkg_part = "_".join(parts[idx:-1])
        base = parts[-1]
    else:
        pkg_part = "_".join(parts[:-1])
        base = parts[-1]
    return f"{pkg_part}__{base}.txt"

def main():
    if not (PROJECT_ROOT / "app").exists():
        print("❌ 当前目录不是 CreditCardManager 根（没找到 app/）")
        return

    if DEST.exists():
        for f in DEST.glob("*.txt"):
            f.unlink()
    DEST.mkdir(exist_ok=True)

    ok, miss = 0, []
    for rel in FILES:
        src = PROJECT_ROOT / rel
        if not src.exists():
            print(f"  [!] 源不存在: {rel}")
            miss.append(rel)
            continue
        dst = DEST / encode_name(rel)
        dst.write_bytes(src.read_bytes())
        print(f"  ✓ {dst.name}  ← {rel}")
        ok += 1

    print(f"\n✅ {ok} 个 → {DEST.resolve()}")
    if miss:
        print(f"⚠️ {len(miss)} 个缺失")
    print(f"\n👉 local/ 里全选 .txt 拖对话发我就行")

if __name__ == "__main__":
    main()