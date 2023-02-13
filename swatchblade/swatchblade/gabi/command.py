import os
import click
import requests
import openshift
import logging
import json
import rich
import csv
import sys

from .. import notice, err, SwatchbladeError

GABI_REQUEST_DEBUG_TMPL = """
gabi url: {{url}} 
gabi headers: {{headers}}
gabi query: {{query_data}}
""".format()

log = logging.getLogger(__name__)


@click.group
@click.option("--gabi-url", type=str)
@click.option("--gabi-token", envvar="OCP_CONSOLE_TOKEN", type=str)
@click.pass_obj
def gabi(obj, gabi_url, gabi_token):
    log.info(obj)
    if gabi_token is None:
        gabi_token = openshift.whoami("-t")
    if not obj.has_config("gabi_token"):
        obj.set_config("gabi_token", gabi_token)

    if gabi_url is None:
        if obj.has_config("gabi_url"):
            gabi_url = obj.get_config("gabi_url")
        else:
            raise SwatchbladeError("No gabi_url provided or found")
    else:
        obj.set_config("gabi_url", gabi_url)

    log.info(obj)
    healthcheck(gabi_url, gabi_token)


@gabi.command()
def export():
    log.info("I am export")


def validate_output(ctx, param, value):
    # TODO
    if value is not None and os.path.exists(value):
        err(f"{value} already exists!")
        raise SwatchbladeError()


def submit_query_request(query, url, token):
    """Submit the query to gabi in the expected shape"""
    query_data = {"query": query}
    headers = {"Authorization": f"Bearer {token}"}
    q_url = f"{url}/query"
    log.info("Submitting query to gabi")
    log.debug(GABI_REQUEST_DEBUG_TMPL.format(url=q_url, headers=headers, query_data=json.dumps(query_data)))
    resp = requests.post(q_url, headers=headers, json=query_data)
    if not resp.ok:
        raise GabiError(f"HTTP {resp.status_code} {resp.reason} : {resp.text}")

    data = resp.json()
    if data["error"]:
        raise GabiError(data["error"])

    return data



def process_results(out_file_name, fieldsep, data, format_csv=True):
  """Process gabi json result into csv"""
  if data is not None and data["result"] is not None:
      out = sys.stdout if not out_file_name else open(out_file_name, "wt")
      try:
          if format_csv:
              log.info(f"Writing {len(data['result'])} CSV records to {out_file_name or 'STDOUT'}")
              writer = csv.writer(out, quoting=csv.QUOTE_ALL, delimiter=fieldsep)
              writer.writerows(data["result"])
          else:
              log.info(f"Writing JSON data to {out_file_name or 'STDOUT'}")
              out.write(json.dumps(data["result"]))
      finally:
          out.flush()
          out.close()
  else:
      log.info("No results returned")

@gabi.command()
@click.option("-q", "--query", required=True, type=str)
@click.option("-o", "--output", callback=validate_output, type=str)
@click.option("-t", "--result-type", default="JSON", type=click.Choice(["JSON", "CSV"], case_sensitive=False))
@click.pass_obj
def query(obj, output, query, result_type):
    notice(f"output is {output} and query is {query}")
    data = submit_query_request(query, obj.get_config("gabi_url"), obj.get_config("gabi_token"))

    if result_type == "JSON":
        rich.print(data)
    else:
        process_results()


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
