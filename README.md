LogGenerator – Parallel Computing Exercise
Two programs are included in a single solution:

CreateLogs: Generate 3,000 log files, each containing 20,000 lines, with the format log_dd_MM_yy.txt.
SearchLogs: Find lines containing the keyword "login by 99" and write results to ketqua.txt in the form: fileName - lineNumber.
System Requirements
OS: Windows (Visual Studio 2022)
.NET: .NET 6.0+ (Console App)
RAM/CPU: Sufficient for heavy I/O; uses multi-threading for performance
Project Structure
Solution: LogGenerator
Project1: LogGenerator.CreateLogs (Console App)
Project2: LogGenerator.SearchLogs (Console App)
Environment Setup
Install .NET 6.0+ (if not already installed).
Open Visual Studio 2022 and load the LogGenerator solution.
Building and Running
Run Program 1: Create Logs
Open project LogGenerator.CreateLogs.
Build Solution (Ctrl+Shift+B).
Run Main (F5 or Ctrl+F5).
Result: a directory named logs is created under the working directory, containing 3,000 log files, each with about 20,000 lines.
Code reference (Main parts provided previously):

Program.cs under namespace LogGenerator.CreateLogs
Run Program 2: Search Logs
Open project LogGenerator.SearchLogs.
Build Solution (Ctrl+Shift+B).
Run Main (F5 or Ctrl+F5).
Result: ketqua.txt is written in the working directory with lines in the format:
fileName - lineNumber
Notes:

Ensure the logs directory exists before running the search.
For about 60 million lines of data, the I/O operation is heavy. The solution uses parallel processing at the file level and a thread-safe collection for results.
How the solution works
CreateLogs

Generates 3000 files named log_dd_MM_yy.txt (and resolves duplicates by appending an index if needed).
Each file contains 20,000 lines with a timestamp, a job/event description, and a random user id.
Approximately 2% of lines include the keyword "login by 99" to ensure some results are present.
SearchLogs

Scans all log_*.txt files in the logs directory.
Uses Parallel.ForEach to process files concurrently.
Reads each file line by line, increments a line counter, and records a match as "filename - lineNumber" in a concurrent collection.
Writes results to ketqua.txt in ascending order.
Algorithm and Optimizations
CreateLogs

Uses a seeded Random to produce deterministic data (can remove seed for variability).
Writes lines sequentially per file to minimize memory usage.
2% probability for including "login by 99" to ensure occurrences without overwhelming output.
SearchLogs

Reads files line by line to avoid loading large files into memory.
Parallelizes at the file level (not within a file) to avoid excessive concurrency on I/O.
Uses ConcurrentBag<string> to safely collect results across threads.
Sorts results before writing to ketqua.txt for deterministic output.
Performance Considerations
Data scale: 3,000 files × 20,000 lines = 60,000,000 lines.
The approach emphasizes file-level parallelism and streaming I/O to manage memory usage and disk I/O effectively.
If you encounter IO bottlenecks, tune the degree of parallelism or process files in batches.
Naming Conventions and Output
Log files: log_dd_MM_yy.txt (and may create log_dd_MM_yy_N.txt if duplicates occur)
Search output: ketqua.txt with lines in the format:
filename.txt - N
Versioning
v1.0: Implemented CreateLogs and SearchLogs as described.
Potential enhancements:
Export results to CSV.
Add error logging and retry mechanisms for IO.
Allow command-line arguments to control the number of files, lines per file, and keyword.
