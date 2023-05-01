// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;

contract DelegateProxy {
    /**
    * @dev Performs a delegatecall and returns whatever the delegatecall returned (entire context execution will return!)
    * @param _dst Destination address to perform the delegatecall
    * @param _calldata Calldata for the delegatecall
    */
    function delegatedFwd(address _dst, bytes memory _calldata) internal {
        assembly {
        switch extcodesize(_dst) case 0 { revert(0, 0) }
        let len := 4096
        let result := delegatecall(sub(gas(), 10000), _dst, add(_calldata, 0x20), mload(_calldata), 0, len)
        switch result case 0 { invalid() }
        return (0, len)
        }
    }
}
