/** 모바일용 회의참여화면 스크립트 */

//소켓IO 클라이언트
const socket = io();
//내 비디오스트림
const myFace = document.getElementById("myFace");
//css hidden속성
const HIDDEN_CN = "hidden";
//API 서버URL
const API_SERVER_URL = 'https://fruity-grapes-bathe-112-148-3-244.loca.lt';

//모바일에서 모듈타입으로 호출할 수 없어서 사용하지 않음
// import { API_SERVER_URL } from './config.js'
// import * as meeting from './meeting.js'

let roomName = "";
let nickname = "";
let roomMeetingId = ""; //회의삭제시 사용하는 회의ID

//회의방에 있는 참가자수 초기화
let peopleInRoom = 1;

let myStream;
let muted = true;
let cameraOff = false;
let myPeerConnection;

let isLogin = false; //로그인여부
let profile = null; //프로필사진

let selectedDeviceId = null; //디바이스에 연결된 카메라device ID를 넣는 변수

//3명이상의 참여자가 발생할 경우,
//각 유저별 SocketId를 키값으로 RTCPeerConnection객체를 생성해서 관리하기 위한 객체선언
let pcObj = {
  // remoteSocketId: pc
};

//3명이상의 참여자가 발생할 경우,
//각 유저별 SocketId를 키값으로 RTCDatachannel객체를 생성해서 관리하기 위한 객체선언
//파일공유 데이터채널 객체
let chObj = {};
//채팅데이터채널 객체
let chatChObj = {};
let maximum = 0; //회의최대 참여인원수


//음소거 비활성화 아이콘 미노출
// unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
// unCameraIcon.classList.add(HIDDEN_CN);



//음소거 버튼 이벤트핸들러
//muteBtn.addEventListener("click", handleMuteClick);
//카메라 버튼 이벤트핸들러
//cameraBtn.addEventListener("click", handleCameraClick);
//카메라선택 이벤트핸들러
// camerasSelect.addEventListener("input", handleCameraChange);

//음소거 이벤트 처리 함수
function handleMuteClick() {
  myStream
    .getAudioTracks()
    .forEach((track) => {
      (track.enabled = !track.enabled);
    });
}

//비디오노출 이벤트 처리 함수
function handleCameraClick() {
  console.log('##### handleCameraClick 함수 호출,,,!');

  myStream
    .getVideoTracks()
    .forEach((track) => (track.enabled = !track.enabled));
}


let isUser = true; //유저를 바라보는 초기카메라 상태
/**
 * 비디오가 변경되었을때 sender라는 것을 통해 track을 업데이트 하는 코드
 * 모바일에서 카메라가 변경되면 변경된 카메라로 화면이 촬영되도록 설정
*/
function handlerMobileCameraChange() {
  console.log('##### handlerMobileCameraChange 호출...!!');
  //처음은 user, 그 이후 클릭은 environment

  isUser = !isUser;
  const mode = isUser ? 'user' : 'environment';

  const cameraConstraints = {
    audio: true,
    video: {
      facingMode: mode,
      // deviceId: { exact: deviceId },
      aspectRatio: { exact: 16/12 },
    },
  };

  navigator.mediaDevices.getUserMedia(cameraConstraints)
      .then(stream => {
        myStream = stream;
        myFace.srcObject = myStream;

        //새 미디어트랙으로 교체하는 함수 실행
        handlerOnReplaceTrack();

        //트랙교체 후 음소거설정이 변경되는 문제 때문에 강제로 음소거되도록 처리
        myStream
          .getAudioTracks()
          .forEach((track) => (track.enabled = muted));
      })
      .catch(e => log(e));
}


/**
 * 연결된 피어객체들에 대해, 새 미디어트랙으로 교체해주는 함수
 */
function handlerOnReplaceTrack() {
  console.log('##### 새 미디어트랙으로 교체함수 실행!!..');
  const count = Object.keys(pcObj).length;

  if (count >= 1) {
    //연결된 피어객체들을 가져온다.
    const PCObject = Object.values(pcObj);

    //변경된 새 비디오트랙을 가져온다.
    const newVideoTrack = myStream.getVideoTracks()[0];

    //Peer객체별로 미디어트랙을 전환하도록 실행
    PCObject.forEach(pcObj => {
      //getSenders(): RTCRtpSender객체의 배열을 반환
      //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타냅니다.
      //Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성.
      const peerVideoSender = pcObj.getSenders()
                              .find((sender) => sender.track.kind == "video");

      //후면 및 전면 카메라로 전환해주는 등 새 미디어트랙으로 전환해준다.
      peerVideoSender.replaceTrack(newVideoTrack);
    })
  }
}


//카메라가 변경되면 변경된 카메라로 화면이 촬영되도록 설정
async function handleCameraChange() {
  try {
    //카메라가 변경될때마다 constraint을 전달받도록 처리함
    //사용할 카메라 정보를 전달한다.

    //1 .상단의 카메라 전환 button을 클릭할때마다, 선택된 devicedID를 가져온다.
    //2. 가져온 deviceID로 옵션을 줘서 stream을 업데이트 한다.
    //3. 피어가 2명 이상의 경우, 상대방의 피어에도 알려준다.
    await getMedia(selectedDeviceId);

    /**
     * @TODO Peer가 하나일때도 RTC객체를 생성되되록 변경해야 적용가능함.
     * socketId도 필요
     */
    //1명만 접속했을 때
    const newVideoTrack = myStream.getVideoTracks()[0];

    //2명 이상 접속했을 때

    if (peerConnectionObjArr.length > 2) {
      //stream의 비디오트랙을 나타내는 MediaStreamTrack 객체의 시퀀스를 반환합니다
      const newVideoTrack = myStream.getVideoTracks()[0];

      peerConnectionObjArr.forEach((peerConnectionObj) => {
        //RTCPeerConnection 객체
        const peerConnection = peerConnectionObj.connection;
        //배열의 각 객체는 하나의 트랙의 데이터의 송신을 담당하는 RTP sender를 나타낸다.
        // Sender 객체는 트랙 데이터의 인코딩과 송신을 확인하고, 조작 할 수 있는 메소드와 속성들을 제공
        const peerVideoSender = peerConnection
          .getSenders()
          .find((sender) => sender.track.kind == "video");

          //후면 및 전면 카메라로 전환해주는 등 새 미디어트랙으로 전환해준다.
          peerVideoSender.replaceTrack(newVideoTrack);
      });
    }

  } catch (error) {
    console.log(error);
  }
}


//회의참여화면 진입시 최초 발생되는 이벤트 처리
callInitPeerMobile();

async function callInitPeerMobile() {

  console.log("##### 모바일회의 참여화면 진입....!");

  if (socket.disconnected) {
    socket.connect();
  }

  //모바일에서 회의참여 화면 진입시 회의정보를 요청하는 소켓이벤트
  socket.emit('request_mobile_meeting_info');

  //media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.
  //await initCall();


}

let maximumFromMobile = 2; //mobile에서 전달된 최대회의참가자수
let userEmail = null;

//signaling server로 부터 회의정보를 전달받는 이벤트
/**
 * meetingId: 회의ID
 * muted: 음소거여부
 * cameraoff: 카메라켜짐 여부
 * myNickname: 회의방에 입장한 유저닉네임
 * maximumFromMobile: 모바일에서 전달된 회의방최대입장인원수
 * userEmail: 사용자아이디(이메일)
 * hostId: 호스트아이디(이메일)
 */
socket.on('response_mobile_meeting_info', (meetingId, mutedValue, cameraOffValue,
    myNickname, maximumFromMobile, userEmail, hostId) => {
  console.log('response_mobile_meeting_info response받음..! muted : %o', mutedValue);
  console.log('response_mobile_meeting_info response받음..! cameraOff : %o', cameraOffValue);
  console.log('response_mobile_meeting_info response받음..! nickname : %o', myNickname);
  console.log('response_mobile_meeting_info response받음..! maximumFromMobile : %o', maximumFromMobile);
  console.log('response_mobile_meeting_info response받음..! userEmail : %o', userEmail);
  console.log('response_mobile_meeting_info response받음..! hostId : %o', hostId);

  //전달된 mute, cameraOff로 값 셋팅
  muted = mutedValue;
  cameraOff = cameraOffValue;


  // media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.
  initCall();



  //채팅작업때문에 나중에 쓰기 위해 주석처리함
  //writeChat(`[${myNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);

  //회의화면에 유저닉네임 셋팅
  const nicknameValue = document.querySelector("#nickname__mobile");
  //회의URL 쿼리파라미터로부터 받은 회의정보를 전역변수에 바인딩
  if (userEmail === hostId) {
    nickname = `${myNickname} (호스트)`;
  } else {
    nickname = myNickname;
  }
  nicknameValue.innerText = nickname;
  roomName = meetingId;


  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  console.log('join_room emit...!!! maximum : %o', maximumFromMobile);
  socket.emit('join_room', meetingId, nickname, maximumFromMobile);

})


function androidCallTest(param) {
  console.log('param call,,,! : %o', param);
}

//화상통화 init함수
async function initCall() {
  await getMedia();
}

//방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.
async function getMedia(deviceId) {
  console.log('1. getMedia call...!');

  //기본 비디오 오디오설정
  //aspectRatio: 비디오 트랙의 종횡비 설명하는 옵션
  const initialConstraints = {
    audio: true,
    video: {
      facingMode: "user",
      aspectRatio: { exact: 16/12 },
    },
  };

  //디바이스정보가 전달될 경우 비디오설정
  //aspectRatio: 비디오 트랙의 종횡비 설명하는 옵션
  const cameraConstraints = {
    audio: true,
    video: {
      deviceId: { exact: deviceId },
      aspectRatio: { exact: 16/12 },
    },
  };

  try {
    //전달된 deviceId값이 있을 경우, 해당 디바이스의 스트림을 노출시킨다(예: 카메라 앞/뒤 화면)
    myStream = await navigator.mediaDevices.getUserMedia(
      deviceId ? cameraConstraints : initialConstraints
    );
    console.log('2. myStream 정보 : %o', myStream);

    // stream을 mute하는 것이 아니라 HTML video element를 mute한다.
    myFace.srcObject = myStream;
    console.log('3. 내 스트림정보를 VIDEO태그에 넣은 후 정보 : %o', myFace.srcObject);

    //팝업해제호출
    dismissLoadingbar();
    //myFace.muted = true;

    //파라미터에서 전달되는 devicedId가 없는경우
    if (!deviceId) {
      console.log('3. getMedia call...! 음소거여부확인: %o', muted);
      console.log('3. getMedia call...! 카메라Off여부확인: %o', cameraOff);

      //모바일에서 전달하는 muted값을 보고 뮤트를 시킨다.
      myStream
        .getAudioTracks()
        .forEach((track) => (track.enabled = !JSON.parse(muted)));
      console.log('4. getMedia call myStream 값 확인 : %o ', myStream);

      //모바일에서 전달하는 cameraOff값을 보고 비디오노출을 처리한다.
      myStream
        .getVideoTracks()
        .forEach((track) => (track.enabled = !JSON.parse(cameraOff)));

      await getCameras();
    }
  } catch (error) {
    console.log(error);
  }
}

//비디오에 노출될 카메라정보를 가져온다.
async function getCameras() {
  try {
    console.log('getMedia에서 getCameras() 호출(비디오에 노출될 카메라 정보 호출...!)');
    const devices = await navigator.mediaDevices.enumerateDevices();
    const cameras = devices.filter((device) => device.kind === "videoinput");
    console.log('cameras : %o', cameras);

    //디바이스에 연결된 카메라 정보를 가져온다.
    cameras.forEach((camera) => {
      selectedDeviceId = camera.deviceId;
      console.log('selectedDeviceId 값 확인 : %o', selectedDeviceId);
    });

  } catch (error) {
    console.log(error);
  }
}

//참가인원 초과했을 경우 홈으로 이동시킴
socket.on("reject_join", () => {
  // Paint modal
  //paintModal("참가인원이 초과하여 회의에 입장할 수 없습니다!");
  //location.replace(`${window.location.origin}/reject`);
  Android.joinReject();

  // Erase names
  const nicknameContainer = document.querySelector("#nickname__mobile");
  nicknameContainer.innerText = "";
  roomName = "";
  nickname = "";
});

// Socket call
// 다른사람이 방에 들어올 경우 빋는 이벤트(예를들어 peer A가 받는 이벤트)
socket.on("welcome", async (userObjArr) => {
  // console.log("somebody join...!");

  //같은방에 참가한 유저정보를 가져온다
  const length = userObjArr.length;
  if (length === 1) {
    return;
  }

  console.log(`회의에 입장한 유저수: ${length}`);
  console.log('회의에 입장한 유저객체: : %o', userObjArr)

  //stream이 생성되기도 전에 미디어정보(myStream)를 가져오는 문제로
  //settimeout에 0.8초 설정
  setTimeout(() => {
    makePeerConnection(length, userObjArr);
  }, 900);

})

//각 피어별 RTCPeerConnection객체를 생성해 시그널링이 이뤄지도록 처리하는 함수
async function makePeerConnection(length, userObjArr) {
  //방금 입장한 유저를 제외한 이전유저들(Peer)
  for (let i = 0; i < length - 1; ++i) {
    try {
      //피어연결을 위한 내 RTCPeerConnection객체를 생성
      //최초입장한 참여자외에는 이 구문에서 PC객체를 생성하게 된다.
      const newPeerConnection = makeConnection(
        userObjArr[i].socketId,
        userObjArr[i].myNickname
      );

      /**
       * 채팅을 위한 DataChannel Offer Side 처리.
       * 3명이상의 참여자가 발생할 경우,
       * 각 유저별 구분가능한 SocketId를 키값으로 RTCDataChannel 객체를 각각 생성해 처리 한다
       * 연결된 상대방Peer의 socketID를 키로 내 datachannel을 넣는다.
       */
      chatChObj[userObjArr[i].socketId] = newPeerConnection.createDataChannel('chat');//채팅채널
      console.log("made file datachannel");

      //answer side
      //내피어 객체에서 데이터채널 이벤트를 받도록 설정.
      newPeerConnection.ondatachannel = (event) => {
        console.log('B 피어, 채널생성한 피어에서 ondatachannel 이벤트 발생!!!');
        //fileDataChannelCallback(event.channel, remoteSocketId);

        if (event.channel.label === 'chat') {
          console.log('#### 채팅 전송채널');
          chatDataChannelCallback(event.channel);
        }
      };


      //offer로 전달할 SDP생성: 다른 브라우저가 참여할 수 있도록 초대장을 만드는 것
      const offer = await newPeerConnection.createOffer();
      newPeerConnection.setLocalDescription(offer);

      console.log("sent the offer");
      //socketId: 이벤트를 전달할 상대방의 socketId, nickname: 방금입장한 유저
      socket.emit("offer", offer, userObjArr[i].socketId, nickname);

    } catch (err) {
      console.error(err);
    }
  }
}


//원격지에서 받는 offer 이벤트, answer를 생성해 전달한다.
socket.on("offer", async (offer, remoteSocketId, remoteNickname) => {
  //myPeerConnection.addEventListener("datachannel", console.log)
  console.log("received the offer");

  //회의를 최초 입장한 유저의 PC객체
  //remoteNickname: (ex: peerB의 닉네임)
  const newPeerConnection = makeConnection(remoteSocketId, remoteNickname);

  /**
   * DataChannel Offer Side 처리:파일전송을 하기 위해 처리
   * 1:1연결의 경우는 최초입장하는 피어가 PC객체를 생성하지 않는 구조라서,
   * 별도로 createdChannel 생성시킴
  */
  chatChObj[remoteSocketId] = newPeerConnection.createDataChannel('chat');


  /**
   * DataChannel Answer Side 처리
   * 원격지 RTC객체 생성후, 이미 datachannel채널은 생성되었기 때문에 아래처럼 이벤트핸들러 설정
  */
  pcObj[remoteSocketId].addEventListener('datachannel', (event) => {
    console.log('A peer에서 datachannel event listening!!');
    //chObj[remoteSocketId] = event.channel;
    //const chObjValue = ;

    if (event.channel.label === 'chat') {
      console.log('#### 채팅 전송채널');
      chatDataChannelCallback(event.channel);
    }
  })

  //전달된 offer를 setRemoteDescription 시킨다.
  newPeerConnection.setRemoteDescription(offer);
  const answer = await newPeerConnection.createAnswer();
  newPeerConnection.setLocalDescription(answer);

  console.log("sent the answer");
  socket.emit('answer', answer, remoteSocketId);
  //writeChat(`[${remoteNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);

})


// Data Channel (채팅)

//채팅 datachannel로 메세지 이벤트 수신후 콜백 처리
function chatDataChannelCallback(chObjValue) {
  chObjValue.onmessage = (event) => {
    //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
    const {data} = event;
    const param = JSON.parse(data);
    console.log('chatDataChannelCallback param : %o', param);
    writeChat(param.message, MYCHAT_CN, param.remoteNickname,
      '', false, param.remoteProfile, param.remoteId);
  }
}


//peer A에서 전달받은 answer이벤트
//모든 브라우저가 remote, local description을 갖게된다
socket.on('answer', async (answer, remoteSocketId) => {
  console.log("received the answer ");
  /**
   * RTCPeerConnection은 하나의 P2P연결만 수행하기 때문에,
   * 3명이상의 참여자가 발생할 경우,
   * 각 유저별 구분가능한 SocketId를 키값으로 RTCPeerConnection 각각 객체를 생성해
   * SDP응답을 처리한다.
   */
  await pcObj[remoteSocketId].setRemoteDescription(answer);
})


// RTC Code

/**
 * 각 피어 연결을 RTCPeerConnection객체에 의해 처리하는 함수.
 * stream정보를 내 peer에 넣는다.
 * 카메라가 바뀌면 deviceId가 바뀌기 때문에, stream도 같이 업데이트를 해준다.
 * 2명이상의 유저가 회의에 참가했을때, RTCPeerConnection객체를 새로 생성하기 위한 구분값으로 socketId를 전달한다.
*/
function makeConnection(remoteSocketId, remotenickname) {
  console.log('1. makeConnection ...!');

  //RTCPeerConnection -> 로컬 컴퓨터와 원격 피어 간의 WebRTC 연결을 담당해주는 객체
  //공용IP정보를 가져오기 위해 STUN서버 설정
  //장치에 공용주소를 알려주는 코드
  myPeerConnection = new RTCPeerConnection({
    iceServers: [
      {
        urls: [
      "stun:stun.l.google.com:19302",
      "stun:stun1.l.google.com:19302",
      "stun:stun2.l.google.com:19302",
      "stun:stun3.l.google.com:19302",
      "stun:stun4.l.google.com:19302",
        ],
      }
    ]
  });

  console.log('2. makeConnection ...!: %o', myPeerConnection);

  //myPeerConnection 객체 생성 후에, icecandidate 이벤트를 받도록 설정한다.
  //애뮬레이터에서 발생되지 않는 문제 재발생
  myPeerConnection.addEventListener("icecandidate", (e) => {
    handleIce(e, remoteSocketId);
  });

  //ICE agent상태가 변경될때 발생하는 이벤트
  myPeerConnection.oniceconnectionstatechange = function(event) {
    if (myPeerConnection.iceConnectionState === 'connected') {

      //RTCPeerConnection 인터페이스 메소드
      //RTCRtpTransceiver연결에서 데이터를 보내고 받는 데 사용되는 개체 목록을 반환
      for (let tr of myPeerConnection.getTransceivers()) {

        //현재 선택된 후보 쌍의 두 끝점 구성을 설명하는 개체
        //원격 피어의 구성을 설명하는 local동안 연결의 로컬 끝 구성을 설명합니다.remote
        //후보쌍이 선택되지 않은 경우 null을 반환한다.
        let selected = tr.sender.transport.iceTransport.getSelectedCandidatePair();
        if (selected) {
            //send selected.local to you server...
            console.log(`selected: %o`, selected);
        } else {
          console.log('후보쌍이 선택되지 않은 경우 null을 반환한다.');
        }
      }
    }

    if (myPeerConnection.iceConnectionState === "failed" ||
        myPeerConnection.iceConnectionState === "disconnected" ||
        myPeerConnection.iceConnectionState === "closed") {
      // Handle the failure
      console.log('oniceconnectionstatechange event failed..!!!');
    }
  };


  //addICECandidate()이후 상대방 peer의 stream정보를 추가한다.
  myPeerConnection.addEventListener("addstream", (e) => {
    handleAddStream(e, remoteSocketId, remotenickname);
  });

  console.log('3. makeConnection ...!');

  //peer Connection에 오디오, 미디어 스트림을 넣는다
  myStream
    .getTracks()
    .forEach((track) => {
      myPeerConnection.addTrack(track, myStream);
  })


  console.log('4. makeConnection myStream ...! : %o', myStream);

  /**
   * RTCPeerConnection은 하나의 P2P연결만 수행하기 때문에,
   * 3명이상의 참여자가 발생할 경우,
   * 각 유저별 구분가능한 SocketId를 키값으로 RTCPeerConnection 각각 객체를 생성해 시그널링 처리를 한다
   * 연결된 상대방Peer의 socketID를 키로 내 피어객체를 넣는다.
   */
  pcObj[remoteSocketId] = myPeerConnection;

  //변경된 카메라트랙으로 교체
  handlerOnReplaceTrack();

  ++peopleInRoom;
  sortStreams();
  return myPeerConnection;
}

//브라우저가 각 각 candidate를 주고받는 이벤트를 발생시키는 코드
//연결된 상대방에 대한 socketId를 인자로 받는다(remoteSocketId)
function handleIce(data, remoteSocketId) {
  if(data.candidate){
    console.log("sent candidate : %o", data.candidate);
    socket.emit('ice', data.candidate, remoteSocketId);
  }
}

//상대방의 candidate를 전달받아 addIceCandidate를 실행한다.
socket.on('ice', async (ice, remoteSocketId) => {
  console.log("addIceCandidate...!");

  //remoteSocketId에 연결을 시도하는 내 피어객체에 상대방에게서 전달된 Ice객체를 add한다
  await pcObj[remoteSocketId].addIceCandidate(ice);
})


//연결된 상대의 브라우저 스트림 정보를 받아, 상대방 비디오를 보여주게 된다.
function handleAddStream(data, remoteSocketId, remotenickname) {
  console.log('addStream event fired...!');
  paintPeerFace(data.stream, remoteSocketId, remotenickname);
}

//마지막에 들어오는 피어만 UI변경되도록 카운트 체크
let count = 0;
//회의방에 입장한 다른유저Peer의 비디오stream정보를 받아 DOM에 그리는 함수
//모바일 해상도에 맞춰 피어갯수별로 UI가 달라지도록 처리
function paintPeerFace(peerStream, remoteSocketId, remotenickname) {
  console.log('paintPeerFace 함수 호출...!');
  const streams = document.querySelector(".screen__mobile");
  const div = document.createElement("div");
  div.id = remoteSocketId;
  const video = document.createElement("video");

  //상대방 비디오 불러오기전 노출
  const imgUrl = `${window.location.origin}/public/image/default_user2.png`
  video.poster = imgUrl;

  video.autoplay = true;
  video.playsInline = true;

  //2명일때는 width => 200
  if (peopleInRoom == 2) {
    video.width = "200";
    video.height = "400";
  }

  if (peopleInRoom == 4) {
    video.width = "200";
    video.height = "250";
  }

  video.srcObject = peerStream; //상대방 비디오를 그린다.

  const nicknameContainer = document.createElement("h3");
  nicknameContainer.id = "nickname__mobile";
  nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  div.appendChild(nicknameContainer);
  streams.appendChild(div);
  sortStreams();
}

//비디오스트림UI를 정렬하기 위한 클래스명추가 함수
function sortStreams() {
  const streams = document.querySelector(".screen__mobile");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((stream) => (stream.className = `people${peopleInRoom}`));

  //3명이상일때만 screen__mobile클래스 속성에 flex-wrap: wrap을 준다.
  if (peopleInRoom >= 3) {
    streams.style.flexWrap = 'wrap';
  }

  if (peopleInRoom == 1) {
    const people1Dom = document.querySelector('.people1');
    people1Dom.children[0].width = '400';
    people1Dom.children[0].height = '400';
  }

  if (peopleInRoom == 2) {
    const people2Dom = document.querySelectorAll('.people2');

    //회의를 나갈경우 UI초기화 시켜야해서 아래처럼 처리
    streams.style = '';
    people2Dom.forEach(dom => {
      dom.style = '';
      dom.children[0].width = '200';
      dom.children[0].height = '400';
    })
  }


  //회의참여자가 3명 이상일때 UI처리
  if (peopleInRoom == 3) {
    const people3Dom = document.querySelectorAll('.people3');

    people3Dom.forEach(dom => {
      count++;
      if (count > 2) {
        dom.style.width = '100%';
        dom.style.marginTop = '10px';
        dom.children[0].width = '400';
        dom.children[0].height = '240';
      } else {
        dom.children[0].width = '200';
        dom.children[0].height = '360'
      }
    })
    count = 0;//초기화
  }

  //회의참여자가 4명 이상일땐, 전부wdth 200, 높이 250으로 변경
  if (peopleInRoom == 4) {
    const peopleDom = document.querySelectorAll('.people4');
    peopleDom.forEach(dom => { dom.style = ''; });

    const peopleVideo = document.querySelectorAll('.people4 > video');
    peopleVideo.forEach(dom => {
      dom.width = '200';
      dom.height = '250'; });
  }

  //people5명 이상인 경우, 전부 높이값 변경
  if (peopleInRoom == 5) {
    const people5Dom = document.querySelectorAll('.people5');
    people5Dom.forEach(dom => {
      count++;
      if (count > 4) {
        dom.style.width = '100%';
        dom.style.textAlign = 'center';
        dom.children[0].width = '300';
        dom.children[0].height = '190';
      } else {
        dom.style.textAlign = 'center';
        dom.children[0].width = '170';
        dom.children[0].height = '190';
      }
    })
    count = 0;
  }
}

/** 채팅영역 */


/** 나가기 */

//모바일에서 회의종료팝업의 '확인'버튼클릭 후 발생하는 함수
function leaveRoom(isHost = false, roomMeetingId) {
  console.log("#### server로 leaveRoom 요청이 돌어옴!!!!, host여부 : %o", isHost);
  //roomId가 잘못들어옴
  console.log("#### server로 leaveRoom 방번호 : %o", roomMeetingId);


  /**
   * 회의호스트의 경우 아래의 동작실행
   * (회의종료 팝업은 네이티브에서 미리 띄움)
   * 1) API서버로 회의삭제 요청
   * 2) 회의에 참여중인 다른유저에게 종료 Noti
   * 3) 소켓 커넥션종료
   */
  if (isHost) {
    console.log("#### server로 호스트로부터 요청이 돌어옴!!!! ");
    //회의삭제요청
    deleteMeeting(roomMeetingId)
        .then(result => {
          if(result) { //회의삭제성공
            console.log("#### server에서 회의삭제 성공...! sent finish meeting socket event...!");

            socket.emit('finish_meeting_by_host');
            //socket종료 및 데이터 초기화
            doLeaveRoom();
            return;
          }
        })

  } else  {//호스트가 아닌경우 회의만 나가도록 처리
    //socket종료 및 데이터 초기화
    doLeaveRoom();
  }

}

//socket커넥션을 끊고, 회의방을 나가면서 전역 변수들을 초기화하는 함수
function doLeaveRoom() {
  console.log("#### doLeaveRoom() 함수 호출...!");
  //수동으로 socket 연결을 끊는다.
  socket.disconnect();

  pcObj = [];
  chObj = [];
  peopleInRoom = 1;
  nickname = "";
  muted = true;
  cameraOff = false;


  //트랙을 정지함
  //MediaStreamTrack이 소스(파일, 네트워크 스트림, 장치 카메라와 마이크)를 더는 필요로 하지 않음을 표현
  myStream.getTracks().forEach((track) => track.stop());

  const nicknameContainer = document.querySelector("#nickname__mobile");
  nicknameContainer.innerText = "";

  myFace.srcObject = null;
  //비디오 스트림 화면 삭제
  clearAllVideos();

  //채팅내용 클리어
  //clearAllChat();
}

//socket서버의 leave_room 이벤트를 받고 회의방 나가기 처리
socket.on("leave_room", (leavedSocketId, nickname) => {
  console.log('#### server leave_room listen...!!!!');
  //떠난 참가자의 비디오 삭제
  removeVideo(leavedSocketId);

  //TODO: (채팅)회의에 참여하지 못한 유저 제외하고 회의나갔다는 문구 노출
  // if (nickname) {
  //   writeChat(`[${nickname}] 님이 회의를 나갔습니다.`, NOTICE_CN);
  // }

  --peopleInRoom;
  console.log('peopleInRoom 갯수 확인: %o', peopleInRoom);
  sortStreams();
});

//떠난 참가자의 비디오 삭제
function removeVideo(leavedSocketId) {
  const streams = document.querySelector(".screen__mobile");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    //나가는 참가자의 소켓ID에 해당하는 경우, 그 DOM을 삭제
    if (streamElement.id === leavedSocketId) {
      streams.removeChild(streamElement);
    }
  });
}

//회의방에서 나간 사용자의 스트림UI를 제거
function clearAllVideos() {
  const streams = document.querySelector(".screen__mobile");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    if (streamElement.id != "myStream") {
      streams.removeChild(streamElement);
    }
  });
}


//호스트에 의해 회의가 종료되었다는 이벤트를 받음
socket.on('noti_finish_meeting', () => {

  console.log('#### 호스트에 의해 회의가 종료되엇습니다 이벤트 받음 !!!!');

  //팝업이 뜬뒤 회의종료화면으로 이동
  //paintModal('호스트에 의해 회의가 종료되었습니다. 회의를 나갑니다.', true);

  //모바일에서 해야하는 것
  //1) 호스트에 의해 회의종료 팝업띄우기 -> 팝업이벤트를 네이티브에 전달한다.
  // -> '확인'버튼 클릭 후 이전화면으로 이동
  //2) socket연결 끊고, 회의초기화 doLeaveRoom();


  //1) 안드로이드 interface에 정의한 showPopup를 실행한다.
  showAndroidPopup('호스트에 의해 회의가 종료되었습니다. 회의를 나갑니다.');

  //2) socket연결 끊고, 회의초기화
  doLeaveRoom();

})

function dismissLoadingbar() {
  //stream정보가 맵핑 되었기 때문에, 프로그레스바를 종료시킨다.
  console.log('progress bar 종료...!');
  Android.dismissLoadingbar();
}

function showAndroidPopup(message) {
  console.log('message: %o', message);
  Android.showLeaveMeetingPopup(message);
}


//모바일에서 백버튼 클릭 후 뒤로가기
function leaveFromBackBtn() {
  //수동으로 socket 연결을 끊는다.
  socket.disconnect();
}

// 회의삭제

function deleteMeeting(meetinId) {
  const deleteUri = `${API_SERVER_URL}/api/meeting/info/${meetinId}`;
  return fetch(deleteUri, {
    method: 'DELETE',
  })
  .then(response => {
      if (!response.ok) {
          return response.json()
              .catch(() => {
                  // Couldn't parse the JSON
                  throw new Error(response.status);
              })
              .then(({message}) => {
                  // Got valid JSON with error response, use it
                  throw new Error(message || response.status);
              });
      }
      return response.json();
  })
  .then(result => {
    if (result.statusCode === 200) {
        console.log(result.message);
        return true;
      }
  });
}


// 채팅 처리

const MYCHAT_CN = "myChat";
const NOTICE_CN = "noticeChat";

/**
 *
 * @TODO 여기부터 모바일버전으로 수정하기
 *
*/
//채팅내용을 가져와 채팅LI노드를 만들어 화면에 그린다
function writeChat(message, className = null,
  nickname = null, time = null, mine = false, profile = null, remoteId) {

//time = moment(new Date()).format("h:mm A") //현재시간 오전/오후

//const li = document.createElement("li");

//입장,나가기 노티 메세지UI 그리기
if (className === NOTICE_CN) {
  // const span = document.createElement("span");
  // span.innerText = message;
  // li.appendChild(span);
  // li.classList.add(className);
  // 받는메세지 로그 찍기
  // message
  // Android.showToast(message);

} else {

  console.log('채팅메세지 닉네임 전달값 %o : ', nickname);
  console.log('채팅메세지 message 전달값 %o : ', message);

  //채팅방번호, 채팅내용, 닉네임, 채팅보낸시각, 프로필이미지
  //Android.sendMessage(message);
  Android.receiveChatMessage(message, profile, nickname, remoteId);

  // if(mine){
  //   nickname = `${nickname}[나]`;
  // }

  // let profileImg = '';
  // if(profile) {
  //   profileImg = `${API_SERVER_URL}/api/member/profile/${profile}`;
  // } else {
  //   profileImg = `${window.location.origin}/public/image/default_user.png`;
  // }

  // //일반 채팅메세지UI 그리기
  // li.innerHTML = `<li>
  //     <span class="profile">
  //       <img class="image" src=${profileImg} alt="any" srcset="">
  //     </span>
  //     <div class="message__info">
  //       <span class="user ${mine ? 'mine': ''}">${nickname}</span>
  //       <span class="message">${message}</span>
  //       <span class="time">${time}</span>
  //     </div>
  //   </li>`;

}

//첫 번째 자식 앞에 Node개체 또는 개체 집합을 삽입(chatBox다음에 li노드가 나온다)
//chatBox.appendChild(li);

}

//mobile에서 다른 Peer로 채팅 데이터 전송
function sendChatData(message, profile) {
  const count = Object.keys(pcObj).length;

  if (count >= 1) { //회의에 접속한 인원이 2명 이상의 경우
    const channels = Object.values(chatChObj);
    channels.forEach(channel => {
      sendChat(channel, message, profile);
    })
  }
}

//datachannel로 채팅전송
function sendChat(channel, message, profile = false) {
  if (channel.readyState === 'open') {
    const param = {
      message: message,
      remoteNickname: nickname,
      remoteProfile: profile
    }
    channel.send(JSON.stringify(param));
  }

  //본인화면에서 채팅UI그리도록 처리
  // if (isFrist) {
  //   //내가 작성한 메세지를 내 채팅창에 노출
  //   writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true}, profile);
  // }
}
