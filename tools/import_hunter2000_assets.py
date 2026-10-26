#!/usr/bin/env python3
"""Verificador de recursos Rive V43; importación SVG descontinuada."""
import json,sys
from check_hunter2000_complete import verify
if __name__=="__main__":
    if len(sys.argv)==2 and sys.argv[1]=="--check":
        print(json.dumps(verify(),ensure_ascii=False,indent=2))
    else:
        raise SystemExit("El importador SVG fue retirado. Hunter System V43 utiliza exclusivamente 2000 demos .riv.")
