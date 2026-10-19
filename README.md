# Core Dump Analyzer

A small Java desktop tool that opens Linux **x86-64 ELF core dumps** and shows you what happened at the moment of the crash: which signal was raised, where, the CPU register state, and which file the crashing instruction belongs to.

It parses the ELF note segments directly. No `gdb`, no `libbfd`, and no third-party dependencies are needed. Everything is plain Java and Swing.

> **Status:** beta (v0.9.0). It covers the basics of a crash report and does not try to replace a debugger. See [Limitations](#limitations).



## Features

- Simple GUI: click **IMPORT**, pick a core file, and the report appears. You can import several cores and switch between them in the list.
- **Crash report**: signal name and number, `si_code` with a readable explanation, `errno`, PID, PPID, fault address, RIP, and the mapped file that contains RIP.
- **NT_FILE mapping**: every file-backed memory region (start, end, file offset in pages, path).
- **General purpose registers**: R15 to GS, including `FS_BASE` / `GS_BASE`.
- **RFLAGS** decoded into individual flags: CF, PF, AF, ZF, SF, TF, IF, DF, OF.
- **SIMD**: `MXCSR` and `XMM0` to `XMM15`.
- **FPU (x87)**: control word, status word, tag word, and `ST0` to `ST7`.

### Supported signals

Detailed `si_code` descriptions are provided for:

| Signal | Number |
|---|---|
| `SIGILL` | 4 |
| `SIGTRAP` | 5 |
| `SIGBUS` | 7 |
| `SIGFPE` | 8 |
| `SIGSEGV` | 11 |
| `SIGCHLD` | 17 |
| `SIGPOLL` | 29 |
| `SIGSYS` | 31 |

Any other signal is shown as `UnSupported Signal`. The raw signal number, code and registers are still displayed.

## Requirements

- JDK 17 or newer (the code uses switch expressions, so 14 is the minimum)
- A desktop environment (the tool uses Swing)
- A core dump from a **64-bit little-endian x86-64** Linux process

## Build and run

The sources use the default package, so no build tool is required.

```bash
javac *.java
java CDA
```

Click **IMPORT** and choose a core file. Opening a file from the list re-parses it.

## Getting a core dump to try

Here is a quick way to produce one with a deliberate segfault:

```bash
cat > crash.c << 'EOF'
int main(void) {
    volatile int *p = 0;
    *p = 42;
    return 0;
}
EOF
gcc -g -o crash crash.c

ulimit -c unlimited
./crash        # Segmentation fault (core dumped)
```

Where the core file ends up depends on your system's `core_pattern`:

```bash
cat /proc/sys/kernel/core_pattern
```

- If it is a plain file name such as `core`, the dump is in the current directory.
- If it pipes to `systemd-coredump`, use `coredumpctl dump ./crash -o core.crash` to extract it. The extracted file may be compressed, so decompress it first if needed.

## How it works

1. `ELFParser` validates the ELF header (ELF64, little-endian) and walks the program headers.
2. For each `PT_NOTE` segment it reads the notes and keeps the ones it needs:

   | Note | Used for |
   |---|---|
   | `NT_PRSTATUS` | PID, PPID, general purpose registers |
   | `NT_SIGINFO` | signal number, `errno`, `si_code`, fault address |
   | `NT_FILE` | file-backed memory mappings |
   | `NT_PRFPREG` | x87 and SSE state (512-byte FXSAVE layout); optional |

3. The results are packed into a `CoreDumpData` object, which builds the text reports shown by `GUI`.

The crashing thread is taken to be the **first `NT_PRSTATUS`** in the file. The Linux kernel writes the crashing thread's status first.

RIP is matched against the `NT_FILE` ranges to tell you which binary or shared library the crash happened in.

## Project layout

| File | Role |
|---|---|
| `CDA.java` | Entry point; starts the GUI |
| `GUI.java` | Swing window and report panels |
| `ELFParser.java` | Reads the ELF header and notes |
| `CoreDumpData.java` | Holds parsed data; builds report text and decodes signal codes |
| `PrStatus.java` | PID, PPID, registers |
| `SigInfo.java` | Signal info |
| `NTFileInfo.java` | File mappings |
| `PrFPreg.java` | x87 / SSE registers |

## Limitations

- **x86-64 Linux only.** The register layout is hard-coded. ARM64 and 32-bit cores are rejected or would be misread.
- **`NT_PRSTATUS`, `NT_SIGINFO` and `NT_FILE` are required.** A core missing any of them is reported as an error. `NT_PRFPREG` is optional: if it is missing, the SIMD and FPU panels show a message instead.
- **Only the crashing thread** is shown. Other threads in the dump are ignored.
- **No AVX / AVX-512 / AMX state** (`NT_X86_XSTATE`). Only XMM0 to XMM15 and the x87 registers are shown.
- **No backtrace, symbols, or memory inspection.** The tool reports the RIP and its containing file, not a function name or stack trace.
- RIP is only matched against **file-backed** mappings. Code in anonymous memory (JIT, for example) shows `Not found in NT_FILE`.
- Fault address is only meaningful for signals that set it (such as `SIGSEGV`, `SIGBUS`, `SIGILL`, `SIGFPE`).
- Signals outside the supported list are shown without a code description.

## Contributing

Issues and pull requests are welcome. Since this is an early prototype, please open an issue first for anything larger than a small fix.

## License

No license has been specified. All rights reserved.
