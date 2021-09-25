var refreshInterval = 2000;

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
                                    .append(generateTemperatureOptions(thermostat.desiredTemperature))))))
    .appendTo($('#thermostats'));
}

function updateThermostat(id) {
  $.get('api/thermostats/' + id, function(thermostat) {
    $('#thermostat_' + thermostat.id + ' .card-header .badge').toggle(thermostat.heaterOn);
    $('#thermostat_' + thermostat.id + ' .card-title').text(formatTemperature(thermostat.ambientTemperature));
    $('#desired_temp_' + thermostat.id).val(thermostat.desiredTemperature).prop('disabled', thermostat.remoteUpdateDisabled);
    setTimeout(updateThermostat, refreshInterval, id);
  });
}

$(document).ready(function() {
  $.get('api/thermostats', function(thermostats) {
    for (var i = 0; i < thermostats.length; ++i) {
      addThermostatCard(thermostats[i]);
      setTimeout(updateThermostat, refreshInterval, thermostats[i].id);
    }
  });
});

