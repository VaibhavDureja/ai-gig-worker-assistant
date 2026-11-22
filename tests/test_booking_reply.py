import unittest

from booking_reply import auto_reply


class AutoReplyTests(unittest.TestCase):
    def test_example_booking_reply(self):
        self.assertEqual(
            auto_reply("Example Customer", "Plumbing", "25 January"),
            "Hi Example Customer, thanks for your request for Plumbing on 25 January. I'll confirm shortly.",
        )

    def test_works_for_another_service(self):
        message = auto_reply("Sample Client", "Electrical repairs", "12 March")
        self.assertIn("Electrical repairs on 12 March", message)
        self.assertTrue(message.startswith("Hi Sample Client,"))

    def test_returns_string_without_sending_message(self):
        self.assertIsInstance(auto_reply("Test Customer", "Design", "1 April"), str)


if __name__ == "__main__":
    unittest.main()
