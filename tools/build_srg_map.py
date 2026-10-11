#!/usr/bin/env python3
"""
构建 SRG -> 官方名 映射表（Minecraft 1.20.1）。

背景：Forge 1.20.1 的生产产物 = 官方类名 + SRG 方法/字段名，
反编译后出现 `m_262824_` / `f_12345_` 这类不可读名字。

⚠️ 重要：SRG 名**不是全局唯一**的 —— 同一个 `m_9236_` 可能出现在多个类里。
只有当所有候选的**官方名完全一致**时，全局文本替换才安全；否则必须丢弃，
交给人工按上下文判断（否则会把 A 类的方法名写到 B 类上）。

输入:
  joined.tsrg            MCP config, TSRG v2, obf -> SRG   (首行: tsrg2 obf srg id)
                         类行   : <obfClass> <srgClass> <id>
                         字段行 : \t<obf> <srg> <id>
                         方法行 : \t<obf> <desc> <srg> <id>
  client.txt server.txt  Mojang 官方映射, ProGuard, obf -> 官方
                         成员行可能带行号前缀，形如 `36:36:net.minecraft...`
输出:
  srg2official.tsv       三列: SRG名 <TAB> 官方名 <TAB> 原始签名

用法:
  python3 build_srg_map.py <joined.tsrg> <client.txt> <server.txt> <out.tsv>
"""
import re
import sys
from collections import defaultdict

PRIM = {'V': 'void', 'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short',
        'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double'}


def desc_to_canon(desc, obf2off):
    """JVM 方法描述符 -> (返回类型, (参数类型...))，类名换成官方名。"""
    def one(s, i):
        dims = 0
        while s[i] == '[':
            dims += 1
            i += 1
        c = s[i]
        if c == 'L':
            j = s.index(';', i)
            raw = s[i + 1:j]
            # 未混淆的库类（如 java/lang/String）在 ProGuard 里写作 java.lang.String
            name = obf2off.get(raw, raw.replace('/', '.'))
            i = j + 1
        else:
            name = PRIM[c]
            i += 1
        return name + '[]' * dims, i

    i = 1
    params = []
    while desc[i] != ')':
        t, i = one(desc, i)
        params.append(t)
    ret, _ = one(desc, i + 1)
    return ret, tuple(params)


def parse_proguard_sig(sig):
    """ProGuard 成员签名 -> (是否方法, canon, 成员名)。"""
    sig = re.sub(r'^\d+:\d+:', '', sig.strip())
    m = re.match(r'^(.+?)\s+([\w$<>]+)\s*\((.*)\)$', sig)          # 方法
    if m:
        params = tuple(p.strip() for p in m.group(3).split(',') if p.strip())
        return True, (m.group(1).strip(), params), m.group(2)
    m = re.match(r'^(.+?)\s+([\w$<>]+)$', sig)                      # 字段
    if m:
        return False, (m.group(1).strip(), ()), m.group(2)
    return None


def main():
    if len(sys.argv) != 5:
        print(__doc__)
        sys.exit(1)
    tsrg_path, client, server, out = sys.argv[1:5]

    # ---- 1. 官方映射 ----
    obf2off = {}
    # (obfClass, obfName) -> {(is_method, canon): (官方名, 原始签名)}
    # 用 dict 而非 list：client.txt 与 server.txt 有重复条目，去重后 len 判断才可靠
    by_member = defaultdict(dict)
    for path in (client, server):
        cur = None
        with open(path, encoding='utf-8') as f:
            for line in f:
                if line.startswith('#') or not line.strip():
                    continue
                if not line[0].isspace():
                    m = re.match(r'^(.+) -> (.+):\s*$', line.strip())
                    if m:
                        obf2off[m.group(2)] = m.group(1)
                        cur = m.group(2)
                    continue
                if cur is None:
                    continue
                m = re.match(r'(.+) -> (\S+)\s*$', line.strip())
                if not m:
                    continue
                parsed = parse_proguard_sig(m.group(1))
                if parsed:
                    by_member[(cur, m.group(2))][(parsed[0], parsed[1])] = (parsed[2], m.group(1))
    print(f"[1/3] 官方映射: {len(obf2off)} 类 / {len(by_member)} 成员键")

    # ---- 2. joined.tsrg ----
    srg_names = defaultdict(set)    # srg -> {官方名}
    srg_raw = {}                    # srg -> 原始签名（留档）
    cur = None
    with open(tsrg_path, encoding='utf-8') as f:
        for line in f:
            if line.startswith('tsrg2') or not line.strip():
                continue
            if not line[0].isspace():
                cur = line.split(' ', 1)[0]
                continue
            if line.startswith('\t\t'):
                # 参数行: \t\t<序号> <kind> <srg参数名> <id>
                p = line.strip().split(' ')
                if len(p) == 4 and p[0].isdigit() and p[2].startswith('p_'):
                    srg_names[p[2]].add(f"arg{p[0]}")
                    srg_raw.setdefault(p[2], f"param index {p[0]}")
                continue
            parts = line.strip().split(' ')
            if len(parts) == 4:              # 方法: obf desc srg id
                obfname, desc, srg = parts[0], parts[1], parts[2]
                try:
                    want_canon, want_method = desc_to_canon(desc, obf2off), True
                except Exception:
                    continue
            elif len(parts) == 3:            # 字段: obf srg id
                obfname, srg = parts[0], parts[1]
                want_canon, want_method = None, False
            else:
                continue

            cands = by_member.get((cur, obfname), {})
            if not cands:
                continue
            name = raw = None
            if want_canon is not None:                      # 方法：签名精确匹配
                hit = cands.get((want_method, want_canon))
                if hit:
                    name, raw = hit
            if name is None:                                # 兜底：同类候选名字唯一
                same_kind = [v for k, v in cands.items() if k[0] == want_method]
                names = {v[0] for v in same_kind}
                if len(names) == 1:
                    name = next(iter(names))
                    raw = same_kind[0][1]
            if name is None:
                continue
            srg_names[srg].add(name)
            srg_raw.setdefault(srg, raw)

    safe = {s: next(iter(n)) for s, n in srg_names.items() if len(n) == 1}
    conflict = {s: n for s, n in srg_names.items() if len(n) > 1}
    print(f"[2/3] SRG->官方: 安全可用 {len(safe)} 条；同名冲突 {len(conflict)} 条（已丢弃）")
    for s, n in list(conflict.items())[:5]:
        print(f"    冲突示例 {s}: {sorted(n)}")

    with open(out, 'w', encoding='utf-8') as f:
        for srg, name in safe.items():
            f.write(f"{srg}\t{name}\t{srg_raw.get(srg, '')}\n")
    print(f"[3/3] 已写出 {out}")


if __name__ == '__main__':
    main()
