import datetime
import json
from pprint import pprint
import click
import console_link.logic as logic
from console_link.logic.instantiation import Environment
from console_link.models.metrics_source import Component, MetricStatistic


#################### UNIVERSAL #################### noqa: E266


class Context(object):
    def __init__(self, config_file) -> None:
        self.config_file = config_file
        self.env = Environment(config_file)
        self.json = False


@click.group()
@click.option(
    "--config-file", default="/etc/migration_services.yaml", help="Path to config file"
)
@click.option("--json", is_flag=True)
@click.pass_context
def cli(ctx, config_file, json):
    ctx.obj = Context(config_file)
    ctx.obj.json = json


###################### CLUSTERS ###################  noqa: E266


@cli.group(name="clusters")
@click.pass_obj
def cluster_group(ctx):
    if ctx.env.source_cluster is None:
        raise ValueError("Source cluster is not set")
    if ctx.env.target_cluster is None:
        raise ValueError("Target cluster is not set")


@cluster_group.command(name="cat-indices")
@click.pass_obj
def cat_indices_cmd(ctx):
    """Simple program that calls `_cat/indices` on both a source and target cluster."""
    if ctx.json:
        click.echo(
            json.dumps(
                {
                    "source_cluster": logic.clusters.cat_indices(
                        ctx.env.source_cluster, as_json=True
                    ),
                    "target_cluster": logic.clusters.cat_indices(
                        ctx.env.target_cluster, as_json=True
                    ),
                }
            )
        )
        return
    click.echo("SOURCE CLUSTER")
    click.echo(logic.clusters.cat_indices(ctx.env.source_cluster))
    click.echo("TARGET CLUSTER")
    click.echo(logic.clusters.cat_indices(ctx.env.target_cluster))
    pass


###################### REPLAYER ###################  noqa: E266


@cli.group(name="replayer")
@click.pass_obj
def replayer_group(ctx):
    if ctx.env.replayer is None:
        raise ValueError("Replayer is not set")


@replayer_group.command(name="start")
@click.pass_obj
def start_replayer_cmd(ctx):
    logic.services.start_replayer(ctx.env.replayer)


###################### METRICS ###################  noqa: E266


@cli.group(name="metrics")
@click.pass_obj
def metrics_group(ctx):
    if ctx.env.metrics_source is None:
        raise ValueError("Metrics source is not set")


@metrics_group.command(name="list")
@click.pass_obj
def list_metrics_cmd(ctx):
    if ctx.json:
        click.echo(json.dumps(ctx.env.metrics_source.get_metrics()))
        return
    pprint(ctx.env.metrics_source.get_metrics())


@metrics_group.command(name="get-data")
@click.argument("component", type=click.Choice([c.value for c in Component]))
@click.argument("metric_name")
@click.option(
    "--statistic",
    type=click.Choice([s.name for s in MetricStatistic]),
    default="Average",
)
@click.option("--lookback", type=int, default=60, help="Lookback in minutes")
@click.pass_obj
def get_metrics_data_cmd(ctx, component, metric_name, statistic, lookback):
    # TODO: this should go through a logic class
    starttime = datetime.datetime.now() - datetime.timedelta(minutes=lookback)

    click.echo(f"Component: {component}")
    click.echo(f"Metric Name: {metric_name}")
    click.echo(f"Statistic: {statistic}")
    click.echo(f"Start Time: {starttime.isoformat()}")
    click.echo(f"Metrics Source Type: {type(ctx.env.metrics_source)}")
    pprint(
        ctx.env.metrics_source.get_metric_data(
            Component[component.upper()],
            metric_name,
            MetricStatistic[statistic],
            startTime=starttime,
        )
    )


#################################################

if __name__ == "__main__":
    cli()
