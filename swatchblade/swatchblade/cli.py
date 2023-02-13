import click
import logging

from . import __version__, init_log_level, notice, err
from .gabi.command import gabi


log = logging.getLogger(__name__)


class SwatchContext:
    def __init__(self, *, log_level: str):
        init_log_level(log_level)
        self.config = {}

    def has_config(self, key):
        return key in self.config

    def get_config(self, key):
        return self.config[key]

    def set_config(self, key, value):
        self.config[key] = value
        notice("Config:")
        notice(f"  {key} = {value}")


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
    log.debug("Verbose logging is enabled.")

    for key, value in config:
        ctx.obj.set_config(key, value)


cli.add_command(gabi)


if __name__ == "__main__":
    cli()
