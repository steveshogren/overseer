var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
    AdminItem = require('./adminwrapper.jsx'),
    actionCreator = require('./studentactioncreator'),
    SwipeHelpers = require('./swipeHelpers.jsx'),
    studentStore = require('./StudentStore');

var today = studentStore.getToday();

module.exports = React.createClass({
    getInitialState: function () {
        return {students: []};
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange);
        this.setState({students: studentStore.getStudents(true)});
    },
    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onChange);
    },
    signIn: function (student) {
        this.refs.missingSwipeCollector.validateSignDirection(student, 'in');
    },
    signOut: function (student) {
        this.refs.missingSwipeCollector.validateSignDirection(student, 'out');
    },
    isSigningIn: function(student) {
        return !student.last_swipe_date || student.last_swipe_type === 'out' || !student.last_swipe_date.startsWith(studentStore.getToday())
    },
    getSwipeButton:  function(student, way){
        var buttonIcon = 'fa-arrow-right';
        if (way === 'out') {
            buttonIcon = 'fa-arrow-left';
        }
        var iclassName = "fa " + buttonIcon + " sign-" + student._id;
        if(this.isSigningIn(student)) {
            return <button onClick={this.signIn.bind(this, student)} className="btn btn-sm btn-primary"><i className={iclassName}>&nbsp;</i></button>;
        }else{
            return <button onClick={this.signOut.bind(this, student)} className="btn btn-sm btn-info"><i className={iclassName}>&nbsp;</i></button>;
        }
    },
    getStudent: function(student, way){
        var link = <Link to="student" params={{studentId: student._id}} id={"student-" + student._id}>{student.name}</Link>;
        var button = this.getSwipeButton(student, way);
        return <div className="panel panel-info student-listing col-sm-11">
            <div >{link}</div>
            <div className="attendance-button">
                {button}
            </div>
        </div>;
    },
    render: function () {
        var absentCol = [],
            notYetInCol = [],
            inCol = [],
            outCol = [];

        var students = this.state.students;
        students.sort((a, b) => {
            return (a['name'] > b['name'] ? 1 : -1);
        });
        students.map(function(student){
            if (!student.in_today && student.absent_today) {
                absentCol.push(this.getStudent(student, 'absent'));
            }
            else if (!student.in_today && !student.absent_today) {
                notYetInCol.push(this.getStudent(student, 'notYetIn'));
            }
            else if (student.in_today && student.last_swipe_type === 'in') {
                inCol.push(this.getStudent(student, 'in'));
            }
            else if (student.in_today && student.last_swipe_type === 'out') {
                outCol.push(this.getStudent(student, 'out'));
            }
        }.bind(this));


        return <div className="row">
            <SwipeHelpers ref="missingSwipeCollector"></SwipeHelpers>
            <div className="row student-listing-table">
                <div className="col-sm-3 column">
                    <div className="panel panel-info absent">
                        <div className="panel-heading absent"><b>Not Coming In ({absentCol.length})</b></div>
                        <div className="panel-body row">{absentCol}</div>
                    </div>
                </div>
                <div className="col-sm-3 column not-in">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>Not Yet In ({notYetInCol.length})</b></div>
                        <div className="panel-body row">{notYetInCol}</div>
                    </div>
                </div>
                <div className="col-sm-3 column in">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>In ({inCol.length})</b></div>
                        <div className="panel-body row">{inCol}</div>
                    </div>
                </div>
                <div className="col-sm-3 column out">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>Out ({outCol.length})</b></div>
                        <div className="panel-body row">{outCol}</div>
                    </div>
                </div>
            </div>
        </div>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
});
