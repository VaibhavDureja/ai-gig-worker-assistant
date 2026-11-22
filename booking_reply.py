def auto_reply(name, service, date):
    """Return a polite booking acknowledgement for a customer."""
    return (
        f"Hi {name}, thanks for your request for {service} on {date}. "
        "I'll confirm shortly."
    )


# This uses a fictional customer label, not the creator's name.
if __name__ == "__main__":
    reply = auto_reply("Example Customer", "Plumbing", "25 January")
    print(reply)
