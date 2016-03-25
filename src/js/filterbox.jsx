var React = require('react');

var component = React.createClass({
  filterChanged: function(event) {
    this
      .props
      .onFilterChange(event.target.value);
  },
  render: function() {
    return <div style={{padding: "5px"}} className="panel ">
      <form style={{textAlign: "center"}} className="form-inline">
        <input
          style={{width: "90%"}}
          type="text"
          placeholder="Filter..."
          className="form-control"
          ref="filterText"
          onChange={this.filterChanged}/>
      </form>
    </div>;
  }
});

module.exports = component;
