# RideSync Benchmark Application

This is a buggy RideSync application designed as a benchmark for testing bug detection and fixing skills. The application contains 20 intentional bugs (T01-T20) that need to be identified through unit tests and fixed in separate commits.

## Overview

RideSync is a real-time ride-sharing application that allows groups to track each other's locations during rides. The application includes:

- User registration and authentication
- Group management and invitations
- Real-time location tracking via WebSocket
- GPS data processing via Kafka
- Anomaly detection (stationary alerts, direction drift)
- REST APIs for location updates

## Bugs Included

The application contains 20 intentional bugs across different areas:

### Security Issues (T01-T03, T07, T13)
- **T01**: Broken User Registration – No password hashing, duplicate emails allowed
- **T02**: Invite via Email lacks Admin role check – Anyone can send invites
- **T03**: Insecure Location API – No JWT/session validation
- **T07**: WebSocket allows join without group check
- **T13**: Location data leakage – unrestricted API query

### Session Management (T04, T05, T09)
- **T04**: Multiple active sessions per user – duplicate map markers
- **T05**: Kafka consumer processes inactive sessions
- **T09**: Processes updates when ride inactive

### Data Processing (T06, T08, T10-T12, T19)
- **T06**: Malformed GPS payload crashes consumer
- **T08**: Marker mismatch – using deviceId not userId
- **T10**: Stationary alerts false positive due to GPS jitter
- **T11**: Direction drift miscalculation
- **T12**: Alert message template broken
- **T19**: False alerts in stop-and-go traffic

### Performance Issues (T14-T18, T20)
- **T14**: Latency due to fallback polling
- **T15**: Over-broadcasting to all clients
- **T16**: Inefficient nearby query – no bounding box filter
- **T17**: Crash on Kafka drop – no retry
- **T18**: Slow anomaly detection – blocking IO
- **T20**: Hardcoded stationary threshold

## Project Structure

```
src/
├── main/java/com/ridesync/
│   ├── model/           # Domain entities
│   ├── dto/             # Data transfer objects
│   ├── repository/      # JPA repositories
│   ├── service/         # Business logic (with bugs)
│   ├── controller/      # REST controllers and WebSocket handlers
│   ├── consumer/        # Kafka consumers
│   └── config/          # Configuration classes
└── test/java/com/ridesync/
    ├── service/         # Service layer tests
    ├── controller/      # Controller tests
    ├── consumer/        # Consumer tests
    └── BugDetectionTestSuite.java  # Comprehensive bug detection tests
```

## How to Use This Benchmark

### 1. Run the Tests
```bash
mvn test
```

The tests are designed to **fail** and expose the bugs. Each test demonstrates a specific problem.

### 2. Identify the Bugs
Review the failing tests to understand what each bug does:
- Read the test code to understand the expected vs actual behavior
- Check the service/controller code to see the buggy implementation
- Use the bug descriptions in the table above

### 3. Fix the Bugs
For each bug (T01-T20):
1. Create a new branch: `git checkout -b fix-T01`
2. Write a unit test that demonstrates the bug
3. Fix the bug in the code
4. Ensure the test passes
5. Commit the fix: `git commit -m "Fix T01: Implement password hashing and email validation"`

### 4. Run All Tests
After fixing each bug, run the full test suite to ensure no regressions:
```bash
mvn test
```

## Learning Objectives

This benchmark helps you practice:

1. **Bug Detection**: Writing tests that expose bugs
2. **Security**: Implementing proper authentication and authorization
3. **Data Validation**: Handling malformed data gracefully
4. **Performance**: Optimizing queries and reducing latency
5. **Real-time Systems**: Managing WebSocket connections and Kafka consumers
6. **Error Handling**: Implementing retry mechanisms and circuit breakers
7. **Configuration**: Externalizing hardcoded values
8. **Testing**: Writing comprehensive unit and integration tests

## Expected Outcomes

After completing this benchmark, you should be able to:

- Identify security vulnerabilities in web applications
- Implement proper authentication and authorization
- Handle real-time data processing with error resilience
- Optimize database queries and WebSocket performance
- Write comprehensive test suites for bug detection
- Apply best practices for Spring Boot applications

## Notes

- Each bug is intentionally simple to understand but represents real-world issues
- The fixes should follow Spring Boot best practices
- Consider both functional and non-functional requirements
- Some bugs may have multiple valid solutions
- Focus on understanding the root cause, not just the symptoms
