# Sample App

This application runs some initialization logic to find `UserTransaction`
and insert it into a known location in JNDI.

## Instructions

To run the sample, invoke `./gradlew clean libertyStart`.

To stop the server again, invoke `./gradlew libertyStop`.

To verify it ran correctly, find the `messages.log` file and search for the string `usertran`.

**Note: different invocations may be required if using the Windows Command Prompt. It is recommended to use a bash shell if possible.**
