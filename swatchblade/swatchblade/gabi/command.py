import click
import requests
import openshift
import logging


GABI_REQUEST_DEBUG_TMPL = """
gabi url: {{url}} 
gabi headers: {{headers}}
gabi query: {{query_data}}
""".format()


log = logging.getLogger(__name__)


@click.group
@click.option("--gabi-url", required=True, type=str)
@click.pass_obj
def gabi(obj, gabi_url):
    log.info(obj)


    gabi_token = openshift.whoami("-t")


    healthcheck(gabi_url, gabi_token)


@gabi.command()
def export():
    log.info("I am export")


@gabi.command()
@click.option("-")
def query():

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

    log.debug("Gabi is available")

    return True
