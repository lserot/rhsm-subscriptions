import logging
import rich.logging

__version__ = "0.1.0"

#    "[%(asctime)s] [%(levelname)s] "
#    "[%(filename)s:%(funcName)s:%(lineno)d] - %(message)s"
LOG_FMT = "%(message)s"
LOG_DATEFMT = "%Y-%m-%d %H:%M:%S"


def init_log_level(log_level):
    level = getattr(logging, log_level) if log_level else logging.INFO
    logging.basicConfig(
        level=level,
        format=LOG_FMT,
        datefmt="[%X]",
        handlers=[rich.logging.RichHandler()],
    )


def notice(msg):
    rich.print(f"[yellow]{msg}[/yellow]")


def err(msg):
    rich.print(f"[bold red]{msg}[/bold red]")


class SwatchbladeError(Exception):
    """Errors specific to swatchblade"""
    pass
