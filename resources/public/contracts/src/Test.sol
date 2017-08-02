pragma solidity ^0.4.14;

contract TestTwo {
    function() payable {
        revert();
    }

    function buy() payable {
        revert();
    }
}

contract TestOne {

    TestTwo testTwo;
    address testTwoAddr;

    function Test(address _testTwo) {
        testTwo = TestTwo(_testTwo);
        testTwoAddr = _testTwo;
    }

    function buy() payable {
        testTwoAddr.transfer(1);
//        testTwoAddr.buy(1);
//        if (!testTwoAddr.send(1)) {
//            throw;
//        }
    }
}
