import socket
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    s.bind(("", 8080))
    print("PORT 8080 BIND SUCCESSFUL")
    s.close()
except Exception as e:
    print("PORT 8080 BIND FAILED:", e)
