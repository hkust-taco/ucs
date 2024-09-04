# OOPSLA 2024 Artifact for _The Ultimate Conditional Syntax_

Our paper introduces a new expressive conditional syntax called _Ultimate Conditional Syntax_ (hereinafter referred to as UCS). In the paper, we propose an algorithm to translate this syntax to traditional pattern matching and prove its correctness.

Our artifact implements this syntax and its translation algorithm on the MLscript compiler. The artifacts consists of two parts:

1. _The main project_ is a Scala project, which is a complete MLscript compiler, and includes the implementation and tests of UCS;
2. _The web demo_ provides a user-friendly interface, allowing people to compile and run MLscript (with UCS) programs directly in the browser, and view the results of each stage of the algorithm described in the paper.

The main project is the paper's main contribution, which fully implements the algorithm specified by the paper. The web demo illustrates the reusability of our main project: it can be reused by other programs (even in different programming languages).

**For more instructions, including how to run the main project and web demo, please refer to the documentation at `manual/manual.pdf`.**
This document is a comprehensive guide to the artifact,
and it is based on the documentation that was reviewed by reviewers from [OOPSLA 2024 Artifact Evaluation](https://2024.splashcon.org/track/splash-2024-oopsla-artifacts).
