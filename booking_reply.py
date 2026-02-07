def auto_reply(name, service, date):
    message = f"Hi {name}, thanks for your request for {service} on {date}. I’ll confirm shortly."
    return message


# Example usage
reply = auto_reply("John", "Plumbing", "25 January")
print(reply)
