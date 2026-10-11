#!/usr/bin/env python3
"""
把反编译产物里的 SRG 名（m_xxxxx_ / f_xxxxx_）批量替换成官方名。

用法:
  python3 apply_srg_map.py <srg2official.tsv> <目标目录>
"""
import os
import re
import sys
from collections import Counter

SRG_RE = re.compile(r'(?<![\w$])([mfp])_(\d+)_(?![\w$])')


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    tsv, root = sys.argv[1], sys.argv[2]

    srg2name = {}
    with open(tsv, encoding='utf-8') as f:
        for line in f:
            p = line.rstrip('\n').split('\t')
            if len(p) >= 2:
                srg2name[p[0]] = p[1]
    print(f"映射表: {len(srg2name)} 条")

    files = []
    for dirpath, _, names in os.walk(root):
        for n in names:
            if n.endswith('.java'):
                files.append(os.path.join(dirpath, n))
    print(f"待处理文件: {len(files)} 个")

    replaced = Counter()
    unresolved = Counter()
    total_repl = 0

    for path in files:
        with open(path, encoding='utf-8') as f:
            src = f.read()

        def sub(m):
            nonlocal total_repl
            srg = m.group(0)
            if srg in srg2name:
                replaced[srg] += 1
                total_repl += 1
                return srg2name[srg]
            unresolved[srg] += 1
            return srg

        new = SRG_RE.sub(sub, src)
        if new != src:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new)

    print(f"\n替换成功: {total_repl} 处，涉及 {len(replaced)} 个不同 SRG 名")
    print(f"未命中  : {sum(unresolved.values())} 处，涉及 {len(unresolved)} 个不同 SRG 名")
    if unresolved:
        print("\n未命中 Top 20:")
        for srg, c in unresolved.most_common(20):
            kind = {'m': '方法', 'f': '字段', 'p': '参数'}.get(srg[0], '?')
            print(f"  {srg}  ({kind})  x{c}")
    print(f"\n映射表命中率: {len(replaced) / max(1, len(replaced) + len(unresolved)) * 100:.1f}%")


if __name__ == '__main__':
    main()
