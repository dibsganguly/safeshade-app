import re,subprocess,sys,time
needle=sys.argv[1].lower(); out=sys.argv[2]
for attempt in range(3):
    subprocess.run(["adb","shell","uiautomator","dump","/sdcard/ui.xml"],capture_output=True)
    subprocess.run(["adb","pull","/sdcard/ui.xml",out],capture_output=True)
    try: x=open(out,encoding="utf-8",errors="ignore").read()
    except Exception: x=""
    exact=None; loose=None
    for node in re.finditer(r'<node [^>]*>',x):
        n=node.group(0)
        t=re.search(r' text="([^"]*)"',n); d=re.search(r' content-desc="([^"]*)"',n); b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n)
        if not b: continue
        labels=[(t.group(1) if t else ""),(d.group(1) if d else "")]
        x1,y1,x2,y2=map(int,b.groups()); c=((x1+x2)//2,(y1+y2)//2)
        for l in labels:
            ll=l.lower()
            if ll==needle or ll.startswith(needle+",") : exact=exact or c
            elif needle in ll: loose=loose or c
    hit=exact or loose
    if hit: print(hit[0],hit[1]); sys.exit(0)
    time.sleep(1)
print("NOTFOUND")
