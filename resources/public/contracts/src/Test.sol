pragma solidity ^0.4.11;

contract TestTwo {
    function() payable {
        revert();
        throw;
    }

    function buy() payable {
        throw;
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
