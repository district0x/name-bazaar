pragma solidity ^0.4.11;

import "SafeMath.sol";
import "OfferingLibrary.sol";

library EnglishAuctionOfferingLibrary {
    using SafeMath for uint;
    using OfferingLibrary for OfferingLibrary.Offering;

    struct EnglishAuctionOffering {
        uint  startPrice;
        uint  startTime;
        uint  endTime;
        uint  extensionDuration;
        uint  extensionTriggerDuration;
        uint  minBidIncrease;
        uint  winningBid;
        address  winningBidder;
    }

    event onBid(address indexed bidder, uint amount, uint endTime, uint datetime);
    event onSettingsChanged(uint startPrice, uint endTime, uint extensionDuration, uint extensionTriggerDuration, uint minBidIncrease);

    function construct(
        EnglishAuctionOffering storage self,
        uint _startPrice,
        uint _startTime,
        uint _endTime,
        uint _extensionDuration,
        uint _extensionTriggerDuration,
        uint _minBidIncrease
    ) {
        self.startPrice = _startPrice;
        self.startTime = _startTime < now ? now : _startTime;
        require(_endTime > self.startTime);
        self.endTime = _endTime;
        self.extensionDuration = _extensionDuration;
        self.extensionTriggerDuration = _extensionTriggerDuration;
        require(_minBidIncrease > 0);
        self.minBidIncrease = _minBidIncrease;
    }

    function bid(EnglishAuctionOffering storage self) {
        require(self.startTime >= now);
        require(now < self.endTime);

        if (self.winningBid == 0) {
            require(msg.value >= self.startPrice);
        } else {
            require(msg.value >= self.winningBid.add(self.minBidIncrease));
            self.winningBidder.transfer(self.winningBid);
        }

        self.winningBidder = msg.sender;
        self.winningBid = msg.value;

        if ((self.endTime - self.extensionTriggerDuration) <= now) {
            self.endTime = now.add(self.extensionDuration);
        }

        onBid(msg.sender, self.winningBid, self.endTime, now);
    }

    function finalize(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(now > self.endTime);
        require(self.winningBidder != 0x0);
        offering.finalize(self.winningBidder, self.winningBid);
    }

    function reclaim(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        if (offering.isSenderEmergencyMultisig()) {
            if (!hasNoBids(self) && !offering.isEmergencyDisabled()) {
                self.winningBidder.transfer(self.winningBid);
            }
        } else {
            require(hasNoBids(self));
        }
        offering.reclaim();
    }

    function setSettings(
        EnglishAuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _extensionTriggerDuration,
        uint _minBidIncrease
    ) {
        require(offering.isSenderOriginalOwner());
        require(hasNoBids(self));

        construct(
            self,
            _startPrice,
            self.startTime,
            _endTime,
            _extensionDuration,
            _extensionTriggerDuration,
            _minBidIncrease
        );
        onSettingsChanged(
            self.startPrice,
            self.endTime,
            self.extensionDuration,
            self.extensionTriggerDuration,
            self.minBidIncrease
        );
    }

    function hasNoBids(EnglishAuctionOffering storage self) returns(bool) {
        return self.winningBidder == 0x0;
    }


}

