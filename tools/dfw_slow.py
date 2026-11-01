import os, sys, time, json, subprocess
CID="Iv1.b507a08c87ecfe98"; DEV=sys.argv[1]; OUT=os.path.join(os.environ.get("TEMP","."),"gh_token.txt")
def poll():
    payload=f"client_id={CID}&device_code={DEV}&grant_type=urn:ietf:params:oauth:grant-type:device_code"
    for extra in ([], ["-x","http://127.0.0.1:18080"]):
        cmd=["curl","-4","-sS","-X","POST","-H","Accept: application/json","-d",payload,
             "--connect-timeout","12","--max-time","25"]+extra+["https://github.com/login/oauth/access_token"]
        try:
            r=subprocess.run(cmd,capture_output=True,text=True,timeout=40)
            if r.stdout and "access_token" in r.stdout:
                try: return json.loads(r.stdout)["access_token"]
                except Exception: return r.stdout.split('"access_token":"')[1].split('"')[0]
            if r.stdout and "expired_token" in r.stdout: return "EXPIRED"
        except Exception: pass
    return ""
t=time.time(); n=0
while time.time()-t < 780:
    r=poll()
    if r and r!="EXPIRED" and not r.startswith("http"):
        open(OUT,"w",encoding="utf-8").write(r); print(f"TOKEN_OK len={len(r)} polls={n}",flush=True); sys.exit(0)
    if r=="EXPIRED": print("EXPIRED",flush=True); sys.exit(3)
    n+=1; print(f"poll {n}",flush=True); time.sleep(60)
print("TIMEOUT",flush=True)
