import urllib.request
try:
    response = urllib.request.urlopen("http://localhost:8081")
    print("STATUS:", response.getcode())
    print("CONTENT LENGTH:", len(response.read()))
except Exception as e:
    print("ERROR:", e)
