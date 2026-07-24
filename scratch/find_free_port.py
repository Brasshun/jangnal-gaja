import socket

def find_free_port():
    for port in [8081, 8082, 8888, 9000, 9999, 8000]:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            s.bind(("", port))
            s.close()
            return port
        except Exception:
            continue
    return None

print("FREE_PORT:", find_free_port())
