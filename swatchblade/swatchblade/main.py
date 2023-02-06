import click
import logging
import rich.text
import rich.logging

from . import __version__
from . import gabi as gabi_module

#    "[%(asctime)s] [%(levelname)s] "
#    "[%(filename)s:%(funcName)s:%(lineno)d] - %(message)s"
LOG_FMT = "%(message)s"
LOG_DATEFMT = "%Y-%m-%d %H:%M:%S"

log = logging.getLogger(__name__)


def init_log_level(log_level):
    level = getattr(logging, log_level) if log_level else logging.INFO
    logging.basicConfig(
        level=level,
        format=LOG_FMT,
        datefmt="[%X]",
        handlers=[rich.logging.RichHandler()],
    )


def notice(msg):
    log.info(f"[yellow]{msg}[/yellow]", extra={"markup": True})


def err(msg):
    log.info(f"[bold red]{msg}[/bold red]", extra={"markup": True})


class SwatchContext:
    def __init__(self, *, log_level: str):
        init_log_level(log_level)
        self.config = {}

    def set_config(self, key, value):
        self.config[key] = value
        notice("Config:")
        notice(f"  {key} = {value}")


pass_swatch_context = click.make_pass_decorator(SwatchContext, ensure=True)


@click.group()
@click.option(
    "--config",
    nargs=2,
    multiple=True,
    metavar="KEY VALUE",
    help="Overrides a config key/value pair.",
)
@click.option(
    "--verbose", "-v", is_flag=True, default=False, help="Enables verbose mode."
)
@click.version_option(version=__version__)
@click.pass_context
def cli(ctx, config, verbose):
    if verbose:
        level = "DEBUG"
    else:
        level = "INFO"

    ctx.obj = SwatchContext(log_level=level)
    notice("Verbose logging is enabled.")

    for key, value in config:
        ctx.obj.set_config(key, value)


@cli.group()
def gabi():
    pass


gabi.add_command(gabi_module.export)
