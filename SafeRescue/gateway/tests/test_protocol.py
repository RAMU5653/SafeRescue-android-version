import base64
import hashlib
import hmac
import unittest

from gateway.gateway import parse_packet, ReplayGuard


class ProtocolTests(unittest.TestCase):
    def make_packet(self, secret=b"test-secret"):
        unsigned = "SRL1|SOS|device-01|1700000000000|ACTIVE|77|17.5|78.4|12.5|nonce1234"
        tag = base64.b64encode(hmac.new(secret, unsigned.encode(), hashlib.sha256).digest()).decode()
        return (unsigned + "|" + tag).encode()

    def test_valid_packet(self):
        packet = parse_packet(self.make_packet(), b"test-secret", now_ms=1700000000000)
        self.assertEqual(packet.device_id, "device-01")
        self.assertEqual(packet.message_type, "SOS")
        self.assertEqual(packet.battery, 77)

    def test_tampering_is_rejected(self):
        raw = self.make_packet().replace(b"|77|", b"|10|")
        with self.assertRaisesRegex(ValueError, "authentication"):
            parse_packet(raw, b"test-secret", now_ms=1700000000000)

    def test_replay_is_rejected(self):
        packet = parse_packet(self.make_packet(), b"test-secret", now_ms=1700000000000)
        guard = ReplayGuard()
        self.assertTrue(guard.accept(packet, now=100.0))
        self.assertFalse(guard.accept(packet, now=101.0))


if __name__ == "__main__":
    unittest.main()
