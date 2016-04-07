var React = require('react');

module.exports = React.createClass({
    show: function () {
        this.refs.modal.show();
        $(document.body).off('keydown');
        $(document.body).on('keydown', this.handleKeyDown);
    },
    hide: function () {
        this.refs.modal.hide();
    },
    handleKeyDown: function (keypress) {
        if (keypress.keyCode == 27 /*esc*/) {
            this.hide();
            this.unbindEsc();
        }
    },
    unbindEsc: function(){
        $(document.body).off('keydown');
    },
    componentWillUnMount: function () {
        this.unbindEsc();
    },
    render: function () {
        return <div>STUPID</div>;
    }
});

