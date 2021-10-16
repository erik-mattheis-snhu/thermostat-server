/** @author Erik Mattheis <erik.mattheis@snhu.edu> */

/**
 * constructor for an object to maintain a delay value
 * that can get progressively larger with each access
 * 
 * after initial creation or reset(), the next call to get()
 * will return {@link initialDelay}
 * 
 * each subsequent call to get() will return a progressively
 * larger value computed by multiplying the previous value
 * by the {@link backoffFactor}, until {@link maxDelay} is
 * reached, at which point all calls to get() will return {@link maxDelay}
 * until reset() is called
 * 
 * @constructor
 * @param initialDelay - the initial value returned by get()
 * @param maxDelay - the maximum value returned by get()
 * @param backoffFactor - the amount to multiple values by netween successive calls to get()
 */
function RetryDelay(initialDelay, maxDelay, backoffFactor) {

	var _initialDelay = initialDelay;
	var _maxDelay = maxDelay;
	var _backoffFactor = backoffFactor;
	var _delay = initialDelay;

	this.get = function() {
    var delay = _delay;
    if (_delay < _maxDelay) {
      _delay *= _backoffFactor;
      if (_delay > _maxDelay) {
        _delay = _maxDelay;
      }
    }
    return delay;
	};
	
	this.reset = function() {
		_delay = _initialDelay;
	};
}

var historyRetryDelay = new RetryDelay(100, 60000, 2);  // instance for history polling retries
var updateRetryDelay = new RetryDelay(100, 60000, 1.5); // instance for update websocket reconnect

/**
 * formats a temperature by truncating to the nearest tenth and appending '° C'
 *
 * @param temp - the temperature to format
 * @returns the formatted temperature
 */
function formatTemperature(temp) {
  return (Math.floor(temp * 10) / 10).toFixed(1) + '° C';
}

/**
 * creates an options array for the desired temperature drop-down
 *
 * generates options in 0.5 degree intervals from 4.0 through 33.0
 *
 * @param selectedTemp - the value to mark selected when generating options
 * @returns the generated options array
 */
function generateTemperatureOptions(selectedTemp) {
  var options = [];
  for (var temp = 4; temp <= 33; temp += 0.5) {
    options.push($('<option/>', { value: temp, selected: (temp.toFixed(1) == selectedTemp.toFixed(1)), text: temp.toFixed(1) + '°' }));
  }
  return options;
}

/**
 * adds the given thermostat to the page by constructing
 * HTML dynamically and appending it to the thermostats
 * placeholder in the static HTML
 *
 * @param thermostat - the thermostat data retrieved from the API
 */
function addThermostatCard(thermostat) {
  $('<div/>', { class: 'col' })
  .append($('<div/>', { id: 'thermostat_' + thermostat.id, class: 'card' })
          .append($('<h4/>', { class: 'card-header text-white bg-primary bg-gradient', text: thermostat.label })
                  .append($('<span/>', { class: 'badge rounded-pill float-end fw-normal text-uppercase text-light bg-gradient bg-' + (thermostat.heaterOn ? 'danger' : 'secondary'), text: (thermostat.heaterOn ? 'heat' : 'off') })))
          .append($('<div/>', { class: 'card-body text-center' })
                    .append($('<h1/>', { class: 'card-title display-1', text: formatTemperature(thermostat.ambientTemperature) }))
                    .append($('<div/>', { class: 'row justify-content-center' })
                            .append($('<div/>', { class: 'col col-sm-8 col-md-6 col-lg-9 col-xl-8 col-xxl-7' })
                                    .append($('<div/>', { class: 'input-group' })
                                            .append($('<span/>', { class: 'input-group-text pe-2' })
                                                    .append($('<i/>', { class: (thermostat.remoteUpdateDisabled ? 'bi-lock' : 'bi-unlock') })))
                                            .append($('<label/>', { class: 'input-group-text border-start-0 ps-0', for: 'desired_temp_' + thermostat.id, text: 'Desired Temperature' }))
                                            .append($('<select/>', { class: 'form-select', id: 'desired_temp_' + thermostat.id, disabled: thermostat.remoteUpdateDisabled })
                                                    .change(function(event) { changeDesiredTemperature(thermostat.id, $(event.target).val()); })
                                                    .append(generateTemperatureOptions(thermostat.desiredTemperature))))))
                    .append($('<div/>', { class: 'card mt-3' })
                            .append($('<div/>', { class: 'card-body' })
                                    .append($('<div/>', { id: 'temperature_history_' + thermostat.id }))))))
    .appendTo($('#thermostats'));
}

/**
 * makes an AJAX request to the API to set the desired temperature
 * for the specified thermostat
 *
 * @param id - the identifier of the thermostat to update
 * @param temp - the desired temperature to set
 */
function changeDesiredTemperature(id, temp) {
  $.post({
    url: 'api/thermostats/' + id,
    contentType: 'application/json',
    data: JSON.stringify({ desiredTemperature: temp })
  });
}

/**
 * begins periodic polling for temperature history for the specified thermostat
 * over the previous 6 hours via AJAX requests to the API
 *
 * the history data is drawn as a graph within the thermostat
 * card using Plotify
 *
 * @param id - the identifier of the thermostat to get history for
 */
function updateTemperatureHistory(id) {
  var to = new Date();                                      // right now
  var from = new Date(to.getTime() - (6 * 60 * 60 * 1000)); // six hours ago
  $.get({
    url: 'api/thermostats/' + id + '/temperature/history?from=' + from.toISOString() + '&to=' + to.toISOString(),
    success: function(history) {
      var data = [
        {
          x: history.timestamps.map(function(timestamp) { return new Date(timestamp); }), // convert timestamps to Date instances
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
      historyRetryDelay.reset();
      setTimeout(updateTemperatureHistory, 60000, id);
    },
    error: function() {
      setTimeout(updateTemperatureHistory, historyRetryDelay.get(), id);
    }
  });
}

/**
 * connects to the API via websocket to listen for thermostat updates
 *
 * when updates arrive, the HTML is dynamically updated to reflect the
 * latest state
 *
 * @param id - the identifier of the thermostat to get history for
 */
function subscribeToUpdates(id) {
  var ws = new WebSocket('ws://' + window.location.host + '/api/thermostats/' + id + '/updates');
  ws.onmessage = function(event) {
    var thermostat = JSON.parse(event.data);
    // set badge to OFF or HEAT accordingly
    $('#thermostat_' + thermostat.id + ' .card-header .badge').removeClass(thermostat.heaterOn ? 'bg-secondary' : 'bg-danger').addClass(thermostat.heaterOn ? 'bg-danger' : 'bg-secondary').text(thermostat.heaterOn ? 'heat' : 'off');
    // update temperature
    $('#thermostat_' + thermostat.id + ' .card-title').text(formatTemperature(thermostat.ambientTemperature));
    // update desired temperature value, disabling/enabling the drop-down as necessary
    $('#desired_temp_' + thermostat.id).val(thermostat.desiredTemperature).prop('disabled', thermostat.remoteUpdateDisabled);
    // display lock/unlock icon as necessary
    $('#thermostat_' + thermostat.id + ' .input-group i').prop('class', thermostat.remoteUpdateDisabled ? 'bi-lock' : 'bi-unlock');
    updateRetryDelay.reset();
  };
  ws.onerror = function() {
    ws.close();
  };
  ws.onclose = function() {
    setTimeout(subscribeToUpdates, updateRetryDelay.get(), id);
  };
}

// on document load, make AJAX request to API for configured thermostats
$(document).ready(function() {
  $.get('api/thermostats', function(thermostats) {
    for (var i = 0; i < thermostats.length; ++i) { // for each configured thermostat...
      addThermostatCard(thermostats[i]);           //     display thermostat
      updateTemperatureHistory(thermostats[i].id); //     begin polling for temperature history
      subscribeToUpdates(thermostats[i].id);       //     listen for thermostat updates
    }
  });
});
