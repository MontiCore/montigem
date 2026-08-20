/* (c) https://github.com/MontiCore/monticore */
import {Component,ChangeDetectionStrategy, ElementRef, ViewChild} from '@angular/core';
import {OnDestroy} from '@angular/core';
import ApexCharts from 'apexcharts';
import {TimelineData} from "../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimelinecharttypes/TimelineData";
import {GemTimelineChartComponent, config} from "../../../../../../build/gui2java2ts/mc/fenix/charts/GemTimelineChartComponent";
import {Timeline} from "../../../../../../build/gui2java2ts/mc/fenix/charts/gemtimelinecharttypes/Timeline";


@Component({
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [...config.imports],
    selector: config.selector,
    templateUrl: config.templateUrl,
    styles: config.styles,
    styleUrls: [...config.styleUrls]
})
export class GemTimelineChart extends GemTimelineChartComponent implements OnDestroy {

    options: any;
    //targetElement: any;
    chart: ApexCharts;
    @ViewChild('tLChart') targetElement! : ElementRef<HTMLDivElement>;
    private liveData: boolean = true;

    public async init() {
        await super.init();

        this.options = {
            animations: {
                enabled: false,
                speed: 800,
                animateGradually: {
                    enabled: true,
                    delay: 150
                },
                dynamicAnimation: {
                    enabled: true,
                    speed: 350
                }
            },
            noData: {
                text: true ? "Loading...":"No Data present in the graph!",
                align: 'center',
                verticalAlign: 'middle',
                offsetX: 0,
                offsetY: 0,
                style: {
                    color: "#000000",
                    fontSize: '14px',
                    fontFamily: "Helvetica"
                }
            },
            chart: {
                type: 'rangeBar',
                height: 350,
                width: '75%',/**/
                zoom: {
                    autoScaleYaxis: true
                },
                events: {
                    legendClick: function(chartContext, seriesIndex, opts) {
                        this.options.series[seriesIndex].hidden = !this.options.series[seriesIndex].hidden;
                    }.bind(this),
                  click: function(event, chartContext, opts){
                      if(opts.seriesIndex != -1){
                        this.getBlockClick()(this.options.series[opts.seriesIndex].data[opts.dataPointIndex].tlBlock)
                      }
                  }.bind(this)
                }
            },
            plotOptions: {
              bar: {
                horizontal: true
              }
            },
            stroke: {
              width: 1
            },
            fill: {
              type: 'solid',
              opacity: 0.6
            },
            series: [],
            xaxis: {
                type: 'datetime',
                // min: new Date('01 Mar 2012').getTime(),
                tickAmount: 6,
                lables: {
                    format: 'dd MMM yyyy HH:mm:ss',
                    formatter: (value) => {
                        const date: Date = new Date(value);
                        return date.toLocaleDateString("de-DE", {timeZone: "Europe/Berlin"})
                    }
                }
            },
            annotations: {
                yaxis: [],
                xaxis: []
            },

            dataLabels: {
                enabled: false
            },
            markers: {
                size: 0,
                style: 'hollow',
            },
            tooltip: {
                y: {
                    format: 'dd MMM yyyy HH:mm:ss'
                }
            },

        };

        if (this.targetElement) {
            this.chart = new ApexCharts(this.targetElement.nativeElement, this.options); // Neues ApexChart-Objekt instanziieren
            this.chart.render(); // Rendern des Charts
        } else {
            console.error("Konnte das Element mit der ID 'apx-chart' nicht finden.");
        }
    }

    public setData(val: TimelineData) {
        super.setData(val);
        this.processNewData(val);
    }

    id2TimeSeriesIndex: Map<number, number> = new Map();

    private async processNewData(data: TimelineData): Promise<void> {
        if(!data) {
          return;
        }
        for await (const timeline of data.getTimeLineList()) {
            if (this.id2TimeSeriesIndex.has(timeline.getIdentifier())) {
                this.options.series[this.id2TimeSeriesIndex.get(timeline.getIdentifier())] = await this.transformData2Series(timeline);
            } else {
                this.id2TimeSeriesIndex.set(timeline.getIdentifier(), this.options.series.length);
                this.options.series.push(await this.transformData2Series(timeline));
            }
        }

        if (this.liveData) {
            console.log("updateSeries");
            this.chart.updateSeries(this.options.series, false);
        }

    }

    private async transformData2Series(data: Timeline): Promise<{name: string, data: any[], color?: string, hidden?: boolean}> {
        let seriesData = [];
        for await (const dataBlock of data.getBlockList()) {
            seriesData.push(
                {
                    x: dataBlock.getLane(),
                    y: [
                        dataBlock.getStart().getTime(),
                        dataBlock.getEnd().getTime()
                    ],
                    tlBlock : dataBlock

                }
            );
        }

        let newSeries: {name: string, data: any[], color?: string, hidden?: boolean};
        if (data.getColor() == null) {
            newSeries = {name: data.getName(), data: seriesData, hidden: data.isHidden()};
        } else {
            newSeries = {name: data.getName(), data: seriesData, color: data.getColor(), hidden: data.isHidden()};
        }
        return newSeries;
    }



    public setShowAfter(event: any) {
        console.log("setShowAfter", event);

        if (event && event instanceof Date) {
            const date: Date = event;
            this.options.xaxis.min = date.getTime();
            this.chart.updateOptions({
                xaxis: this.options.xaxis
            })
        } else {
            this.options.xaxis.min = null;
            this.chart.updateOptions({
                xaxis: this.options.xaxis
            })
        }
    }


    ngOnDestroy() {
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }
        // run the generated cleanup (route subscription, component destroy)
        super.ngOnDestroy();
    }

  constructor() {
        super();
    }
}

GemTimelineChart["__class"] = "gui.charts.TimeLineChart";
