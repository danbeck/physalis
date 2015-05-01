package object service {
  implicit def function0ToRunnable(f: () => Unit): Runnable =
    new Runnable { def run() = f() }
}