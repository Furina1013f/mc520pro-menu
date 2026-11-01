import os, subprocess, sys, threading, time
URL = sys.argv[1]; OUT = sys.argv[2]; SIZE = int(sys.argv[3]); N = int(sys.argv[4])
def worker(s, e):
    part = f"{OUT}.p{s}"
    pos = s
    for _ in range(120):
        if pos > e: break
        subprocess.run(["curl","-4","-sS","-L","--max-time","240","--connect-timeout","20",
                        "-r",f"{pos}-{e}","-o",part,URL], capture_output=True)
        try: got = os.path.getsize(part)
        except OSError: got = 0
        if got <= 0:
            time.sleep(2); continue
        with open(OUT,"r+b") as f:
            with open(part,"rb") as p:
                f.seek(pos); f.write(p.read())
        pos += got
        os.remove(part)
        if pos > e: break
    try: os.remove(part)
    except OSError: pass
with open(OUT,"wb") as f: f.truncate(SIZE)
chunk = (SIZE + N - 1)//N
ts=[]
for i in range(N):
    s=i*chunk; e=min(SIZE-1, s+chunk-1)
    if s>e: break
    t=threading.Thread(target=worker,args=(s,e),daemon=True); t.start(); ts.append(t)
while any(t.is_alive() for t in ts): time.sleep(5)
for t in ts: t.join()
print("final:", os.path.getsize(OUT), "expect:", SIZE, flush=True)
