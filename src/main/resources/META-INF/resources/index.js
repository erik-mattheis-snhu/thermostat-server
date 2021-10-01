function formatTemperature(temp) {
  return (Math.floor(temp * 10) / 10).toFixed(1) + '° C';
}

function generateTemperatureOptions(selectedTemp) {
  var options = [];
  for (var temp = 4; temp <= 33; temp += 0.5) {
    options.push($('<option/>', { value: temp, selected: (temp == selectedTemp), text: temp.toFixed(1) + '°' }));
  }
  return options;
}

function changeDesiredTemperature(id, temp) {
  $.post({
    url: 'api/thermostats/' + id,
    contentType: 'application/json',
    data: JSON.stringify({ desiredTemperature: temp })
  });
}

function addThermostatCard(thermostat) {
  $('<div/>', { class: 'col' })
  .append($('<div/>', { id: 'thermostat_' + thermostat.id, class: 'card' })
          .append($('<h4/>', { class: 'card-header text-white bg-primary bg-gradient', text: thermostat.label })
                  .append($('<span/>', { class: 'badge rounded-pill float-end fw-normal text-uppercase text-light bg-danger bg-gradient', text: 'heat' }).toggle(thermostat.heaterOn)))
          .append($('<div/>', { class: 'card-body' })
                    .append($('<h1/>', { class: 'card-title display-1', text: formatTemperature(thermostat.ambientTemperature) }))
                    .append($('<div/>', { class: 'input-group w-50' })
                            .append($('<label/>', { class: 'input-group-text', for: 'desired_temp_' + thermostat.id, text: 'Set:' }))
                            .append($('<select/>', { class: 'form-select', id: 'desired_temp_' + thermostat.id, disabled: thermostat.remoteUpdateDisabled })
                                    .change(function(event) { changeDesiredTemperature(thermostat.id, $(event.target).val()); })
                                    .append(generateTemperatureOptions(thermostat.desiredTemperature))))
                    .append($('<div/>', { class: 'card mt-3' })
                            .append($('<div/>', { class: 'card-body' })
                                    .append($('<div/>', { id: 'temperature_history_' + thermostat.id }))))))
    .appendTo($('#thermostats'));
}

function updateTemperatureHistory(id) {
  var to = new Date();
  var from = new Date(to.getTime() - (6 * 60 * 60 * 1000));
  $.get('api/thermostats/' + id + '/temperature/history?from=' + from.toISOString() + '&to=' + to.toISOString(), function(history) {
	var data = [
      {
        x: history.timestamps.map(function(timestamp) { return new Date(timestamp); }),
        y: history.temperatures,
        type: 'scatter',
        line: {
          shape: 'spline',
          simplify: false,
        }
      }
    ];
	var layout = {
	  title: 'Temperature History',
      autosize: true,
      xaxis: {
	    fixedrange: true,
	    tickformat: '%-I:%M %p',
        tickmode: 'auto',
        nticks: 16,
      },
      yaxis: {
	    fixedrange: true,
        tickformat: '.1f',
        ticksuffix: '°',
      }
    };
	var config = {
      displayModeBar: false,
      responsive: true,
    };
	Plotly.newPlot('temperature_history_' + id, data, layout, config);
    setTimeout(updateTemperatureHistory, 60000, id);
  });
}

function subscribeToUpdates(id) {
  var socket = new WebSocket('ws://' + window.location.host + '/api/thermostats/' + id + '/updates');
  socket.onmessage = function(event) {
	var thermostat = JSON.parse(event.data);
    $('#thermostat_' + thermostat.id + ' .card-header .badge').toggle(thermostat.heaterOn);
    $('#thermostat_' + thermostat.id + ' .card-title').text(formatTemperature(thermostat.ambientTemperature));
    $('#desired_temp_' + thermostat.id).val(thermostat.desiredTemperature).prop('disabled', thermostat.remoteUpdateDisabled);
  }
  socket.onclose = socket.onerror = function() {
    subscribeToUpdates(id);
  }
}

$(document).ready(function() {
  $.get('api/thermostats', function(thermostats) {
    for (var i = 0; i < thermostats.length; ++i) {
      addThermostatCard(thermostats[i]);
      updateTemperatureHistory(thermostats[i].id);
      subscribeToUpdates(thermostats[i].id);
    }
  });
});

