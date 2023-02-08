import click
import requests
import os
import logging


GABI_REQUEST_DEBUG_TMPL = "gabi request::{0}  gabi url: {{url}}{0}  gabi headers: {{headers}}{0}  gabi query: {{query_data}}".format(
    os.linesep
)


log = logging.getLogger(__name__)


@click.group
@click.option("--gabi-url", required=True, type=str)
@click.option("--gabi-token", required=True, type=str)
@click.pass_obj
def gabi(ctx, gabi_url, gabi_token):
    log.info(ctx)
    log.info(f"{gabi_url} and {gabi_token}")
    healthcheck(gabi_url, gabi_token)


@gabi.command()
def export():
    log.info("I am export")


class GabiError(Exception):
    """Errors specific to gabi"""

    pass


def healthcheck(url, token):
    """Verify that gabi is available."""
    headers = {"Authorization": f"Bearer {token}"}
    h_url = f"{url}/healthcheck"
    log.debug(GABI_REQUEST_DEBUG_TMPL.format(url=h_url, headers=headers, query_data={}))
    resp = requests.get(h_url, headers=headers)
    if not resp.ok:
        raise GabiError(f"HTTP {resp.status_code} {resp.reason} : {resp.text}")
    data = resp.json()
    if data["status"] != "OK":
        raise GabiError(f"Gabi healthcheck returned {data['status']}")

    log.info("Gabi is available")

    return True
