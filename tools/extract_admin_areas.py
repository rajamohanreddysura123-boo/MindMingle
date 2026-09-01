#!/usr/bin/env python3
"""
Turns a geoBoundaries country file into the name list the district filter needs.

Why this exists: geoBoundariesCGAZ_ADM2.geojson is 550 MB, and the per-country files are ~8 MB
each — almost entirely polygon geometry. The filter does not draw maps and does not do
point-in-polygon; it needs the *names*, which are about 20 KB per country. This throws the geometry
away at extraction time so nothing that large ever reaches Firestore or the app.

Usage:
    python3 tools/extract_admin_areas.py IND IN

Writes shared/src/commonMain/composeResources/files/admin_areas/IN.json, which ships with the app
and is read on demand by the filter sheet. Two arguments because the two catalogues disagree:
geoBoundaries indexes by three-letter code, and a profile's `countryCode` is the two-letter one.

Nothing to seed and nothing to deploy — 12 KB for India's 36 regions and 728 districts, next to the
country list that is already bundled for the phone field. Add a country by running this once and
committing the file.

The output is one file per country:

    { "countryIso3": "IND", "regions": ["Telangana", ...], "districts": ["Hyderabad", ...] }

which the filter sheet reads for the selected country only — one document read, not a dataset.

Two flat lists rather than districts nested under their region: the per-country ADM2 files carry no
ADM1 parent property (only the 550 MB CGAZ file does), so any nesting here would be invented. The
filter treats them as two independent searches, which is also how people search — nobody picks
"Telangana" in order to then find "Hyderabad", they type "Hyderabad".
"""

import json
import os
import sys
import urllib.request

API = "https://www.geoboundaries.org/api/current/gbOpen/{iso3}/{level}/"


def fetch_json(url: str):
    with urllib.request.urlopen(url, timeout=120) as response:
        return json.load(response)


def download(url: str):
    with urllib.request.urlopen(url, timeout=600) as response:
        return json.load(response)


def names_for(iso3: str, level: str) -> list:
    meta = fetch_json(API.format(iso3=iso3, level=level))
    meta = meta[0] if isinstance(meta, list) else meta
    url = meta["simplifiedGeometryGeoJSON"]
    print(f"  {level}: {url}", file=sys.stderr)

    features = download(url)["features"]
    return [
        {
            "name": f["properties"].get("shapeName", "").strip(),
            "id": f["properties"].get("shapeID", ""),
            "parent": f["properties"].get("ADM1_shapeName", "").strip(),
        }
        for f in features
        if f["properties"].get("shapeName")
    ]


RESOURCE_DIR = "shared/src/commonMain/composeResources/files/admin_areas"


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        return 2

    iso3 = sys.argv[1].upper()
    iso2 = sys.argv[2].upper()
    print(f"extracting {iso3}", file=sys.stderr)

    adm1 = names_for(iso3, "ADM1")
    adm2 = names_for(iso3, "ADM2")

    document = {
        "countryCode": iso2,
        "regions": sorted({entry["name"] for entry in adm1 if entry["name"]}),
        "districts": sorted({entry["name"] for entry in adm2 if entry["name"]}),
    }

    os.makedirs(RESOURCE_DIR, exist_ok=True)
    path = os.path.join(RESOURCE_DIR, f"{iso2}.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(document, handle, ensure_ascii=False, separators=(",", ":"))

    size_kb = round(os.path.getsize(path) / 1024, 1)
    print(
        f"{path}: {len(document['regions'])} regions, "
        f"{len(document['districts'])} districts, {size_kb} KB",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
