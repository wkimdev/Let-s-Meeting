const socket = io();
/** 회의방입장 */
const enterRoom = document.querySelector('.enterRoom');


/** 비디오스트림 */
const meetingMain = document.querySelector('.meetingMain');
const myFace = document.getElementById("myFace");


/** 컨트롤영역 */
const muteBtn = document.getElementById("mute");
const muteIcon = muteBtn.querySelector(".muteIcon");//음소거
const unMuteIcon = muteBtn.querySelector(".unMuteIcon");//음소서 비활성화

const cameraBtn = document.querySelector("#camera");
const cameraIcon = cameraBtn.querySelector(".cameraIcon");
const unCameraIcon = cameraBtn.querySelector(".unCameraIcon");
const camerasSelect = document.getElementById("cameras");
const leaveBtn = document.getElementById('leave');

/**파일전송 프로그레스바 */
const progressbar = document.getElementById('progress');
const progressLabel = document.getElementById('progressLabel');

//import test from './fileshare.js';

//css hidden속성
const HIDDEN_CN = "hidden";

//파일전송시 청크최대 사이즈64KB
const MAXIMUM_MESSAGE_SIZE = 65535;
const END_OF_FILE_MESSAGE = 'EOF';


let roomName = "";
let nickname = "";
//회의방에 있는 참가자수 초기화
let peopleInRoom = 1;

let myStream;
let muted = true;
let cameraOff = false;
let myPeerConnection;

let file; //공유할 파일객체
let localReceivedBuffers = []; //로컬에서 파일버퍼를 받기 위한 변수선언
let isLogin = false; //로그인여부

//파일전송 datachannel 처리를 위한 변수선언
let receiveBuffer = [];
let receivedSize = 0;
let receivedFile = {}


//홈화면 입장전에는 미팅화면 보이지 않도록 처리
//meetingMain.classList.add(HIDDEN_CN);

//음소거 비활성화 아이콘 미노출
unMuteIcon.classList.add(HIDDEN_CN);

//카메라 비활성화 아이콘 미노출
unCameraIcon.classList.add(HIDDEN_CN);

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

/**
 * 1)native로 부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 * 2)데탑앱으로부터 방번호,호스트여부 및 음소거 정보 전달받아야 함
 */
//테스트코드 때문에 사용하는 코드
window.addEventListener('DOMContentLoaded', (e) => {
  //TestCode
  // const inputRoom = welcomeForm.querySelector("input#roomName");
  // const inputName = welcomeForm.querySelector("input#nickname");
  // inputRoom.value = 123;
  // inputName.value = 'test';
  // handlWelcomSubmit(e);

  
  //media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
  //callInitPeer();
});

//회의참여화면 진입시 최초 발생되는 이벤트 처리
callInitPeer();

async function callInitPeer(){

  console.log('##### 회의참여화면 진입...! #####');

  if (socket.disconnected) {
    socket.connect();
  }
  await initCall();

  //회의방에 입장한 유저정보요청
  socket.emit('request_meeting_info');

  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  socket.emit('join_room');
}

//회의방에 입장한 유저정보 응답
socket.on('send_userinfo', (myRoomName, myNickname) => {
  console.log('###### 서버로부터 send_userinfo event received,,,!');

  writeChat(`[${myNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);

  //회의화면에 유저닉네임 셋팅
  const nicknameValue = document.querySelector("#userNickname");
  nicknameValue.innerText = myNickname;

  roomName = myRoomName;
  nickname = myNickname;
})




//음소거 버튼 이벤트핸들러
muteBtn.addEventListener("click", handleMuteClick);
//카메라 버튼 이벤트핸들러
cameraBtn.addEventListener("click", handleCameraClick);
//카메라선택 이벤트핸들러
camerasSelect.addEventListener("input", handleCameraChange);

function handleMuteClick() {
  console.log('handleMuteClick...! myStream : %o', myStream);
  
  //test로 주석처리
  myStream
    .getAudioTracks()
    .forEach((track) => {
      //enabled 속성을 사용해 음소거 기능을 구현
      //오디오 track이 비활성화 되면 음소거 상태이다.
      (track.enabled = !track.enabled);
      console.log('track.enabled값 확인 :' + track.enabled);
    });


  if (muted) {//음소거 상태면 미음소거 아이콘 활성화 -> 소리들림
    unMuteIcon.classList.remove(HIDDEN_CN);
    muteIcon.classList.add(HIDDEN_CN);
    muted = false;
  } else {
    muteIcon.classList.remove(HIDDEN_CN);
    unMuteIcon.classList.add(HIDDEN_CN);
    muted = true;
  }
}

function handleCameraClick() {
  myStream //
    .getVideoTracks()
    .forEach((track) => (track.enabled = !track.enabled));
  if (cameraOff) { //카메라실행되도록 아이콘 활성화
    cameraIcon.classList.remove(HIDDEN_CN);
    unCameraIcon.classList.add(HIDDEN_CN);
    cameraOff = false;
  } else {
    unCameraIcon.classList.remove(HIDDEN_CN);
    cameraIcon.classList.add(HIDDEN_CN);
    cameraOff = true;
  }
}

/** 
 * 비디오가 변경되었을때 sender라는 것을 통해 track을 업데이트 하는 코드
 * 캠이 하나밖에 없어서 테스트는 못 함 
 * peerConnectionObjArr을 어디서 설정하는지 몰라 주석처리함(기능은 정상동작함)
*/
async function handleCameraChange() {
  try {
    //방에 입장한 peer의 Stream정보를 가져온다
    //카메라가 변경될때마다 constraint을 전달받도록 처리
    //사용할 카메라 정보를 던진다.
    await getMedia(camerasSelect.value);

    /** 
    if (peerConnectionObjArr.length > 0) {
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

          //카메라를 전환해줌
          peerVideoSender.replaceTrack(newVideoTrack);
      });
    }*/

  } catch (error) {
    console.log(error);
  }
}

//나가기 말고 브라우저를 껐을 경우 disconnected이벤트 듣기
//socket.on()


// Leave Room

function leaveRoom() {
  //모달팝업
  paintModal('정말 회의에서 나가시겠어요?');
}

function doLeaveRoom() {
  //socket 연결을끊난다.
  socket.disconnect();

  enterRoom.classList.remove(HIDDEN_CN); //방입장화면 노출  
  meetingMain.classList.add(HIDDEN_CN); //회의화면 미노출

  welcome.hidden = false;

  pcObj = [];
  chObj = [];
  peopleInRoom = 1;
  nickname = "";

  myStream.getTracks().forEach((track) => track.stop());
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";

  myFace.srcObject = null;
  //비디오 스트림 화면 삭제
  clearAllVideos();

  //채팅내용 클리어
  clearAllChat();
}


//socket서버의 leave_room 이벤트를 받고 회의방 나가기 처리
socket.on("leave_room", (leavedSocketId, nickname) => {
  removeVideo(leavedSocketId);
  writeChat(`[${nickname}] 님이 회의를 나갔습니다.`, NOTICE_CN);
  --peopleInRoom;
  sortStreams();
});

//떠난 참가자의 비디오 삭제
function removeVideo(leavedSocketId) {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    //나가는 참가자의 소켓ID의 경우 해당 DOM삭제
    if (streamElement.id === leavedSocketId) {
      streams.removeChild(streamElement);
    }
  });
}


function clearAllVideos() {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  streamArr.forEach((streamElement) => {
    if (streamElement.id != "myStream") {
      streams.removeChild(streamElement);
    }
  });
}

leaveBtn.addEventListener("click", leaveRoom);

// Modal code

const modal = document.querySelector(".modal");
const modalText = modal.querySelector(".modal__text");
const modalBtn = modal.querySelector(".modal__btn");

function paintModal(text) {
  modalText.innerText = text;
  modal.classList.remove(HIDDEN_CN);

  modal.addEventListener("click", removeModal);
  modalBtn.addEventListener("click", removeModalAndLeave);
  //document.addEventListener("keydown", handleKeydown);
}

function removeModal() {
  modal.classList.add(HIDDEN_CN);
  modalText.innerText = "";
}


//삭제버튼 클릭후 모달삭제
function removeModalAndLeave() {
  console.log('회의종료 버튼 클릭!');
  modal.classList.add(HIDDEN_CN);
  modalText.innerText = "";

  //회의종료 화면으로 이동해야 한다.
  location.href = `${window.location.origin}/leave`;
  //socket종료 및 데이터 초기화
  doLeaveRoom();



  //팝업이 뜨고, 이전화면으로 넘어가야 한다.
  // meetingMain.classList.add(HIDDEN_CN);
  // enterRoom.classList.remove(HIDDEN_CN);
}

//Esc key, Enter클릭 후 모달 삭제
// function handleKeydown(event) {
//   if (event.code === "Escape" || event.code === "Enter") {
//     removeModal();
//     //팝업이 뜨고, 이전화면으로 넘어가야 한다.
//     meetingMain.classList.add(HIDDEN_CN);
//     enterRoom.classList.remove(HIDDEN_CN);
//   }
// }


// Welcom Form (join a room)

const welcome = document.getElementById("welcome");
const welcomeForm = welcome.querySelector("form");

welcomeForm.addEventListener("submit", handlWelcomSubmit)

//회의들어가 버튼입력(submit)후 실행되는 함수
async function handlWelcomSubmit(event){
  event.preventDefault();

  if (socket.disconnected) {
    socket.connect();
  }

  enterRoom.classList.add(HIDDEN_CN); //방입장화면 미노출  
  meetingMain.classList.remove(HIDDEN_CN); //회의화면노출
  
  const inputRoom = welcomeForm.querySelector("input#roomName");
  const inputName = welcomeForm.querySelector("input#nickname");
  
  //media정보를 요청해, 방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
  await initCall();

  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  socket.emit('join_room', inputRoom.value, inputName.value);
  writeChat(`[${inputName.value}] 님이 회의에 참여했습니다.`, NOTICE_CN);

  roomName = inputRoom.value;
  nickname = inputName.value;

  //유저닉네임 셋팅
  const nicknameValue = document.querySelector("#userNickname");
  nicknameValue.innerText = nickname;

  inputRoom.value = "";
  inputName.value = "";
}

//화상통화 init함수
async function initCall() { 
  await getMedia();
}

//방에 입장한 peer의 Stream정보를 가져와 비디오 및 카메라 정보를 그린다.  
async function getMedia(deviceId) {
  console.log('1. getMedia call...!');

  //기본 비디오 오디오설정
  const initialConstraints = {
    audio: true,
    video: { facingMode: "user" },
  };
  //디바이스정보가 전달될 경우 비디오설정 
  const cameraConstraints = {
    audio: true,
    video: { deviceId: { exact: deviceId } },
  };

  try {
    //전달된 deviceId값이 있을 경우, 해당 디바이스의 스트림을 노출시킨다(예: 카메라 앞/뒤 화면)
    myStream = await navigator.mediaDevices.getUserMedia(
      deviceId ? cameraConstraints : initialConstraints
    );

    // stream을 mute하는 것이 아니라 HTML video element를 mute한다.
    myFace.srcObject = myStream;

    if (!deviceId) {
      console.log('2. getMedia call...!');
      // mute default
      myStream.getAudioTracks().forEach((track) => {(track.enabled = false); });
      console.log('3. getMedia call myStream 값 확인 : %o ', myStream);
      
      await getCameras();
    }
  } catch (error) {
    console.log(error);
  }
}

//비디오에 노출될 카메라정보를 가져온다.
async function getCameras() {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const cameras = devices.filter((device) => device.kind === "videoinput");
    const currentCamera = myStream.getVideoTracks();
    //디바이스에 연결된 카메라 정보를 가져온다.
    cameras.forEach((camera) => {
      const option = document.createElement("option");
      option.value = camera.deviceId;
      option.innerText = camera.label;

      if (currentCamera.label == camera.label) {
        option.selected = true;
      }

      camerasSelect.appendChild(option);
    });
  } catch (error) {
    console.log(error);
  }
}

socket.on("reject_join", () => {
  // Paint modal
  paintModal("참가인원이 초과하여 회의에 입장할 수 없습니다!");

  // Erase names
  const nicknameContainer = document.querySelector("#userNickname");
  nicknameContainer.innerText = "";
  roomName = "";
  nickname = "";
});

// Socket call
// 다른사람이 방에 들어올 경우 (예를들어 peer A가 받는 이벤트)
socket.on("welcome", async (userObjArr) => {
  //console.log("join the room : %o", roomName);

  //같은방에 참가한 유저정보를 가져온다
  const length = userObjArr.length;
  if (length === 1) {
    return;
  }

  console.log(`회의에 입장한 유저수: ${length}`);
  console.log('회의에 입장한 유저객체: : %o', userObjArr)

  //방금 입장한 유저를 제외한 이전유저들(Peer)
  for (let i = 0; i < length - 1; ++i) {
    try {
      //피어연결을 위한 내 RTCPeerConnection객체를 생성
      //최초입장한 참여자외에는 이 구문에서 PC객체를 생성하게 된다.
      const newPeerConnection = makeConnection(
        userObjArr[i].socketId,
        userObjArr[i].myNickname
      );

      
      //파일전송을 위한 datachannel생성
      /**
       * DataChannel Offer Side 처리.
       * 3명이상의 참여자가 발생할 경우, 
       * 각 유저별 구분가능한 SocketId를 키값으로 RTCDataChannel 객체를 각각 생성해 처리 한다
       * 연결된 상대방Peer의 socketID를 키로 내 datachannel을 넣는다.
       */
      chObj[userObjArr[i].socketId] = newPeerConnection.createDataChannel('file');//파일공유 채널
      chatChObj[userObjArr[i].socketId] = newPeerConnection.createDataChannel('chat');//채팅채널
      console.log("made file datachannel");
      
      //answer side 
      //내피어 객체에서 데이터채널 이벤트를 받도록 설정.
      newPeerConnection.ondatachannel = (event) => {
        console.log('B 피어, 채널생성한 피어에서 ondatachannel 이벤트 발생!!!');
        //fileDataChannelCallback(event.channel, remoteSocketId);

        if (event.channel.label === 'file') {
          console.log('#### file 전송채널');
          fileDataChannelCallback(event.channel);
        } else if (event.channel.label === 'chat') {
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
});


//원격지에서 받는 offer 이벤트, answer를 생성해 전달한다.
socket.on("offer", async (offer, remoteSocketId, remoteNickname) => {
  console.log("received the offer");

  //회의를 최초 입장한 유저의 PC객체
  //remoteNickname: (ex: peerB의 닉네임)
  const newPeerConnection = makeConnection(remoteSocketId, remoteNickname);

  /** 
   * DataChannel Offer Side 처리:파일전송을 하기 위해 처리
   * 1:1연결의 경우는 최초입장하는 피어가 PC객체를 생성하지 않는 구조라서, 
   * 별도로 createdChannel 생성시킴 
  */
  chObj[remoteSocketId] = newPeerConnection.createDataChannel('file');
  chatChObj[remoteSocketId] = newPeerConnection.createDataChannel('chat');
  

  /** 
   * DataChannel Answer Side 처리
   * 원격지 RTC객체 생성후, 이미 datachannel채널은 생성되었기 때문에 아래처럼 이벤트핸들러 설정
  */
  pcObj[remoteSocketId].addEventListener('datachannel', (event) => {
    console.log('A peer에서 datachannel event listening!!');
    //chObj[remoteSocketId] = event.channel;
    //const chObjValue = ;
    
    //fileDataChannelCallback(event.channel);
    if (event.channel.label === 'file') {
      console.log('#### file 전송채널');
      fileDataChannelCallback(event.channel);
      //testChannelCallback(event);
    } else if (event.channel.label === 'chat') {
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
  writeChat(`[${remoteNickname}] 님이 회의에 참여했습니다.`, NOTICE_CN);
  
})


//test callback
function testChannelCallback (event) {
  console.log('##### testChannelCallback ##### channel callback...!');

  const { channel } = event;
  channel.binaryType = 'arraybuffer';

  const receivedBuffers = [];
  channel.onmessage = async (event) => {
    console.log('##### channel onmessage event received...!!!!');

    const { data } = event;
    try {
      if (data !== END_OF_FILE_MESSAGE) {
        receivedBuffers.push(data);
      } else {
        console.log('1. file send end...!');
        const arrayBuffer = receivedBuffers.reduce((acc, arrayBuffer) => {
          const tmp = new Uint8Array(acc.byteLength + arrayBuffer.byteLength);
          tmp.set(new Uint8Array(acc), 0);
          tmp.set(new Uint8Array(arrayBuffer), acc.byteLength);
          return tmp;
        }, new Uint8Array());
        const blob = new Blob([arrayBuffer]);
        console.log('2. downloadFile2...!');

        downloadFile2(blob, data.split(",")[1]);
        channel.close();
        console.log('3. close channel...!');
      }
    } catch (err) {
      console.log('File transfer failed');
    }
  };

}



//채팅 이벤트 수신후 콜백 처리
function chatDataChannelCallback(chObjValue) {
  chObjValue.onmessage = (event) => {
    //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
    const {data} = event;
    const param = JSON.parse(data);
    writeChat(param.message, MYCHAT_CN, param.remoteNickname);
  }
}

function fileDataChannelCallback(event) {

  console.log('###### received message callback...!');
  console.log(event.data);
}


//datachannel 이벤트 수신후 콜백 처리 - 수정하는 중
function fileDataChannelCallback3(event) {

  console.log('received message callback...!');

  //최초에 보낸 channel.send()이벤트를 받아서 처리하는 구문
  //전역에 선언한 receivedFile값이 없을 경우 파일정보를 먼저 넣는다.
  if(receivedFile['name'] == undefined) {
      console.log('name이 undefined일때');

      if (event.data) {
        const file = JSON.parse(event.data);
        console.log(file);
        receivedFile = file;
        
      }
      

      //돔을 먼저 그려야하나?
      // progressbar.max = file.size;
      // progressbar.value = 0;
      return;
  }

  //////////////////////////////////////////////
  //sendData()에서 보낸 send()이벤트 이후 아래 코드 실행

  console.log('receiveBuffer.push() 실행...!');

  receiveBuffer.push(event.data);
  receivedSize += event.data.byteLength;

  //progressbar.value = receivedSize;

  //message를 받을때마다 파일다운로드 진행상태를 %로 나타내준다
  //progressLabel.innerHTML = 'Receive: ' + (receivedSize/receivedFile.size*100).toFixed(1) + '%';

  if(receivedSize == receivedFile['size']) {
      console.log('receivedSize가 receivedFile[size]와 동일할 때...!');

      const blob = new Blob(receiveBuffer, {type: receivedFile['type']});

      //파일을 다운받을 URL를 생성한다.
      // downloadAnchor.href = URL.createObjectURL(blob);
      // downloadAnchor.download = receivedFile['name'];
      // downloadAnchor.innerHTML = receivedFile['name'];

      downloadFile(blob, receivedFile['name'], nickname);


      //downloadAnchor.click();
      receiveBuffer = [];
      receivedSize = 0;
      receivedFile = {};
  }



}

//datachannel 이벤트 수신후 콜백 처리 - 이전버전
function fileDataChannelCallback2(chObjValue) {
  console.log('##### fileDataChannelCallback ...!!! #####: %o', chObjValue);
  

  ///채널의 기본 예상 바이너리 유형은 Blob이지만 
  //Google Chrome은 이를 지원하지 않으므로 내부적으로 binaryType을 "arraybuffer"로 설정
  // 반면에 Firefox는 Blob을 지원하므로 Blob이 필요
  // 두 브라우저 모두에서 작동하려면 채널의 바이너리 유형을 "배열 버퍼"로 선언하고 Blob을 배열 버퍼로 변환
  chObjValue.binaryType = 'arraybuffer';

  //청크(배열 버퍼)가 오는 대로 저장할 배열
  const receivedBuffers = [];

  //바이트를 받으면 다시 blob을 만들고 다운로드하고 채널을 닫는다
  chObjValue.onmessage = (event) => {
    //const data = JSON.parse(event.data); //파일값을 가져옴
    const { data } = event; //파일값을 가져옴
    //console.log('data 값 확인 : %o', JSON.parse(data));

    try {
      //let parseData = JSON.parse(data);
      if (!IsJsonString(data)) {
        //청크 배열에 추가
        receivedBuffers.push(data);
      } else {

        //"파일 끝" 메시지를 수신하고 청크를 큰 배열 버퍼에 함께 넣는 코드
        const arrayBuffer = receivedBuffers.reduce((acc, arrayBuffer) => {
            const tmp = new Uint8Array(acc.byteLength + arrayBuffer.byteLength);
            tmp.set(new Uint8Array(acc), 0);
            tmp.set(new Uint8Array(arrayBuffer), acc.byteLength);
            return tmp;
          }, new Uint8Array());

          //배열 버퍼를 Blob으로 변환
          const blob = new Blob([arrayBuffer]);

          //Blob을 다운로드할 수 있는 링크를 만든다. 
          //
          let parseData = JSON.parse(data);
          //downloadFile(blob, data.split(',')[1], data.split(',')[2]);
          console.log(`다운로드 파일명 확인: ${parseData.filename} `);
          downloadFile(blob, parseData.filename, parseData.nickname);
          
          //채널을 닫는다.
          //chObjValue.close();
        }


      // JSON to ArrayBuffer
      // const arrayBuffer = new Uint8Array(JSON.parse(parseData.arrayBuffer)).buffer;
      // const blob = new Blob([arrayBuffer]);
      // downloadFile(blob, parseData.filename);

      // chObjValue.close();

    } catch (error) {
      console.error('File transfer failed :' + error);
    }
  };

  
  //console.log('received callback, 상태체크 : %o', chObj[remoteSocketId].readyState);

  //message수신 이벤트
  //chObj[remoteSocketId].onmessage = (event) => handleReceiveMessage(event, remoteSocketId);
  
  //채널이 열리고 닫혔을때 이벤트처리
  //chObj[remoteSocketId].onopen = (event) => handleReceivedChannelStatusChange(event, remoteSocketId);
  //chObj[remoteSocketId].onclose = (event) => handleReceivedChannelStatusChange(event, remoteSocketId);
}


//json type체크
function IsJsonString(str) {
  try {
    var json = JSON.parse(str);
    return (typeof json === 'object');
  } catch (e) {
    return false;
  }
}


//원격 채널에서 "메시지" 이벤트가 발생하면 handleReceiveMessage()메서드가 이벤트 핸들러로 호출
function handleReceiveMessage(event, remoteSocketId) {
  console.log(`received event! ${event.data}`);
  console.log('chObj[' +remoteSocketId+ '] : %o', chObj[remoteSocketId]);


  //파일을 받는 처리 x
  chObj[remoteSocketId].send('hi back!');
}


//원격피어 상태 변경 이벤트 수신 로그
function handleReceivedChannelStatusChange(event, remoteSocketId) {
  if (chObj[remoteSocketId]) {
    console.log("Receive channel's status has changed to " +
        chObj[remoteSocketId].readyState);
  }
}



//peer A에서 전달받은 answer이벤트
//모든 브라우저가 remote, local description을 갖게된다
socket.on('answer', async (answer, remoteSocketId) => {
  console.log("received the answer : %o", answer, remoteSocketId);
  //remoteSocketId에 연결을 시도하는 내 피어객체에 상대방에게서 전달된 SDP를 넣는다.
  await pcObj[remoteSocketId].setRemoteDescription(answer);
})



// RTC Code

/** 
 * 각 피어 연결을 RTCPeerConnection객체에 의해 처리하는 함수.
 * stream정보를 내 peer에 넣는다.
 * 카메라가 바뀌면 deviceId가 바뀌기 때문에, stream도 같이 업데이트를 해준다.
 * 2명이상의 유저가 회의에 참가했을때, 
 * 상대Peer에 대한 socketId를 키값으로 내 PC객체를 처리해 다자간 시그널링이 처리되도록 한다.
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

  console.log('2. makeConnection ...!');

  //myPeerConnection 객체 생성 후에, icecandidate 이벤트를 받도록 설정한다. 
  myPeerConnection.addEventListener("icecandidate", (e) => {
    handleIce(e, remoteSocketId);
  });

  //addICECandidate()이후 상대방 peer의 stream정보를 추가한다.
  myPeerConnection.addEventListener("addstream", (e) => {
    handleAddStream(e, remoteSocketId, remotenickname);
  });

  console.log('3. makeConnection ...!');

  //peer Connection에 오디오, 미디어 스트림을 넣는다
  myStream
    .getTracks()
    .forEach((track) => {
    //다른 유저에게 전송될 트랙들의 묶음에 신규 미디어 트랙을 추가
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
  

  ++peopleInRoom;
  sortStreams();
  return myPeerConnection;
}

//브라우저가 각 각 candidate를 주고받는 이벤트를 발생시키는 코드
//연결된 상대방에 대한 socketId를 인자로 받는다(remoteSocketId)
function handleIce(data, remoteSocketId) {
  if(data.candidate){
    console.log("sent candidate");
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
  console.log("addStream...!");
  paintPeerFace(data.stream, remoteSocketId, remotenickname);
}

//회의방에 입장한 다른유저Peer의 비디오stream정보를 받아 DOM에 그리는 함수
function paintPeerFace(peerStream, remoteSocketId, remotenickname) {
  const streams = document.querySelector(".screen");
  const div = document.createElement("div");
  div.id = remoteSocketId;
  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;
  video.width = "400";
  video.height = "400";
  video.srcObject = peerStream; //상대방 비디오를 그린다.

  const nicknameContainer = document.createElement("p");
  nicknameContainer.id = "userNickname";
  nicknameContainer.innerText = remotenickname;

  div.appendChild(video);
  div.appendChild(nicknameContainer);
  streams.appendChild(div);
  sortStreams();
}


//비디오스트림UI를 정렬하기 위한 클래스명추가 함수
function sortStreams() {
  const streams = document.querySelector(".screen");
  const streamArr = streams.querySelectorAll("div");
  //console.log(`peopleInRoom값 확인 : ${peopleInRoom}`);
  streamArr.forEach((stream) => {
    stream.className = `people${peopleInRoom}`;
  });
}


/** 채팅영역 */
const chatForm = document.querySelector(".chatForm");
const chatBox = document.querySelector(".chat__box");

const MYCHAT_CN = "myChat";
const NOTICE_CN = "noticeChat";

//send버튼 클릭후 submit액션

chatForm.addEventListener("submit", handleChatSubmit);

//채팅내용을 datachaneel로 전송하는 함수
function handleChatSubmit(event) {
  console.log('채팅 datachannel 전송 event call...!');
  event.preventDefault();

  const chatInput = chatForm.querySelector("input");
  const message = chatInput.value;
  chatInput.value = "";

  if (message) {
    //파일업로드DOM을 한번만 그리기 위한 구분값
    let isFrist = true;

    const count = Object.keys(pcObj).length;
    console.log(`count값 확인 : %o`, count);
    
    if (count === 0) { //한명만 접속했을 경우
      //내가 작성한 메세지를 내 채팅창에 노출
      writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true});
    } else if (count >= 1) {
      const channels = Object.values(chatChObj);
      channels.forEach(channel => {
        sendChat(channel, message, isFrist);
        isFrist = false;
      })
    }
  }
}
//datachannel로 채팅전송
function sendChat(channel, message, isFrist) {
  if (channel.readyState === 'open') {
    const param = {
      message: message,
      remoteNickname: nickname
    }
    channel.send(JSON.stringify(param));
  }

  //본인화면에서 채팅UI그리도록 처리
  if (isFrist) {
    //내가 작성한 메세지를 내 채팅창에 노출
    writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true});
  }
}


//채팅내용을 socket서버로 전송하는 함수
function handleChatSubmit2(event) {
  console.log('채팅 send 버튼 클릭 후 submit event call...!');
  event.preventDefault();
  const chatInput = chatForm.querySelector("input");
  const message = chatInput.value;
  chatInput.value = "";

  //소켓서버에 채팅내용, 채팅을 작성한 유저명, 방번호, 시간을 전송
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후
  socket.emit("chat", message, roomName, nickname, time);
  console.log('emit chat event...!');

  //내가 작성한 메세지를 내 채팅창에 노출
  writeChat(`${message}`, MYCHAT_CN, nickname, '', { mine: true});
}

//채팅내용을 가져와 채팅LI노드를 만들어 화면에 그린다
function writeChat(message, className = null, nickname = null, time = null, mine = false) {
  console.log('writeChat 호출...!');

  time = moment(new Date()).format("h:mm A") //현재시간 오전/오후

  const li = document.createElement("li");

  //입장,나가기 노티 메세지UI 그리기
  if (className === NOTICE_CN) {
    const span = document.createElement("span");
    span.innerText = message;
    li.appendChild(span);
    li.classList.add(className);

  } else {
    if(mine){
      nickname = `${nickname}[나]`;
    }

    //미로그인의 경우 디폴트 이미지가 노출되도록 처리
    let profileImg = '';
    if(isLogin) {
      profileImg = '';
    } else {
      profileImg = `${window.location.origin}/public/image/default_user.png`;
    }

    //일반 채팅메세지UI 그리기
    li.innerHTML = `<li>
        <span class="profile">
          <img class="image" src=${profileImg} alt="any" srcset="">
        </span>
        <div class="message__info">
          <span class="user ${mine ? 'mine': ''}">${nickname}</span>
          <span class="message">${message}</span>
          <span class="time">${time}</span>
        </div>
      </li>`;

  }

  //첫 번째 자식 앞에 Node개체 또는 개체 집합을 삽입(chatBox다음에 li노드가 나온다)
  chatBox.appendChild(li);

}

socket.on("chat", (message, remoteNickname, time) => {
  console.log('chat event received from server...!');
  //내가 작성한 메세지를 상대방 브라우저의 채팅창에 노출
  writeChat(message, MYCHAT_CN, remoteNickname, time);
});

//채팅내용 클리어
function clearAllChat() {
  const chatArr = chatBox.querySelectorAll("li");
  chatArr.forEach((chat) => chatBox.removeChild(chat));
}

// 파일공유

//버튼 클릭후 파일공유 화면이 뜬다.
//파일을 선택하면 해당 파일을 불러온다. 

//하단 파일공유 버튼 
const shareFileBtn = document.querySelector('.fileShare');
const cancleBtn = document.getElementById('cancel-button');
const selectFileInput = document.getElementById('select-file-input');

shareFileBtn.addEventListener('click', () => {
  document.getElementById('select-file-dialog').style.display = 'block';
});

document.getElementById('select-file-input').addEventListener('change', (event) => {
  file = event.target.files[0];
  document.getElementById('ok-button').disabled = !file;
});

cancleBtn.addEventListener('click', () => {
  document.getElementById('select-file-input').value = '';
  document.getElementById('select-file-dialog').style.display = 'none';
});

selectFileInput.addEventListener('change', (event) => {
  file = event.target.files[0];
  document.getElementById('ok-button').disabled = !file;
});


//파일을 전송을 위해, 피어간 데이터채널 객체를 분기처리 하는 함수
const shareFile = () => {
  console.log('#### 1) shareFile ');
  if (file) {
    //파일업로드DOM을 한번만 그리기 위한 구분값
    let isFrist = true;

    const count = Object.keys(pcObj).length;

    if (count === 0) {//한명만 접속했을 경우
      console.log('#### 2) 한명!!! ');

      file.arrayBuffer().then(buffer => {
        console.log('#### 2) 한명일 경우 전달!!! ');
        let arrayBuffer = buffer;
        //1.Blob 메서드 arrayBuffer 를 사용하여 파일을 배열 버퍼로 변경
        for (let i = 0; i < arrayBuffer.byteLength; i += MAXIMUM_MESSAGE_SIZE) {
          //2.그런 다음 전체 파일이 올 때까지 청크를 하나씩 보낸다.
          let arrayBufferValue = arrayBuffer.slice(i, i + MAXIMUM_MESSAGE_SIZE);
          localReceivedBuffers.push(arrayBufferValue);
          //console.log('i:'+ i + " : " + arrayBufferValue);
        }

        createFlieDownloadDom(file.name, {isOnePeer: true});

      }).catch(error => {
        console.error(error);
        showAlert('fail');
      });
      

    } else if (count >= 1) {
      console.log('#### 2) 두명이상의 경우 전달!!! ');
      const channels = Object.values(chObj);
      channels.forEach(channel => {
        doShareFile(channel, isFrist);
        //sampleShareFile(channel, isFrist);
        isFrist = false;
      })
    }
  }
};



//파일전송을 처리 함수
function doShareFile(channel, isFrist) {
  console.log('#### 3) doShareFile 전달함수 실행!!!, channel값 : %o', channel);

  channel.binaryType = 'arraybuffer';
  
  //"낮음(low)"으로 간주되는 버퍼링된 나가는 데이터의 바이트 수를 지정합니다. 기본값은 0입니다.
  channel.bufferedAmountLowThreshold = 0;


  if (channel.readyState === 'open') {
    //파일전송
    console.log('#### 4) 채널open이후 함수전달!!!');


    //프로그래스바 태그에 요소가 나타내는 작업에 필요한 작업량속성을 준다.
    //ex: max="100" value="70"이면 프로그래스바에 70%정도로 표기됨
    // progressbar.max = file.size;
    //요소가 나타내는 작업을 완료한 양
    // progressbar.value = 0;

    //프로그래스 상태값을 나타내주는 라벨값 (0~100%)
    // progressLabel.innerHTML = 0;


    //1)데이터채널로 파일정보 전달
    // -> 다른피어의 datachannel이벤트를 발생시켜 onReceiveMessageCallback()함수를 실행하게된다.
    // channel.send(JSON.stringify({

    //     name: file.name,
    //     size: file.size,
    //     type: file.type,
    // }));
    channel.send('나의파일을 받아줘!!!!!!!!!!!!!');
    console.log('datachannel send....!');

    //sendData(channel);

    closeDialog();


    
    // file.arrayBuffer().then(buffer => {
    //   //console.log('result : %o', result);
      
    //   let arrayBuffer = buffer;
    //   //대용량파일 전송 처리구문
    //   //1.Blob 메서드 arrayBuffer 를 사용하여 파일을 배열 버퍼로 변경
    //   for (let i = 0; i < arrayBuffer.byteLength; i += MAXIMUM_MESSAGE_SIZE) {
    //     //2.그런 다음 전체 파일이 올 때까지 청크를 하나씩 보낸다.
    //     let arrayBufferValue = arrayBuffer.slice(i, i + MAXIMUM_MESSAGE_SIZE);
    //     channel.send(arrayBufferValue);
    //     localReceivedBuffers.push(arrayBufferValue);
    //   }
      
    //   // array buffer to JSON
    //   // const dataString = JSON.stringify(Array.from(new Uint8Array(arrayBuffer)));

    //   //파일명과 arraybuffer 데이터를 전달
    //   const fileInfoData = {
    //     "filename": file.name,
    //     "nickname": nickname
    //   }

    //   //3.마지막으로 "파일 끝, 파일명" 메시지를 보낸다.
    //   //channel.send(END_OF_FILE_MESSAGE + "," + file.name + "," + nickname);
    //   channel.send(JSON.stringify(fileInfoData));

    //   closeDialog();

    //   //파일전송 성공 다이알로그 보였다가 사라지게
    //   showAlert('success');
    // }).catch(error => {
    //   console.error(error);
    //   closeDialog();
    //   showAlert('fail');
    // });

  }

  //본인화면에서 파일업로드 돔그리도록 처리 -> 한번만 그리도록 변경
  if (isFrist) {
    createFlieDownloadDom(file.name)
  }

  channel.onmessage = (event) => {
    console.log('채널생성한 피어) onmessage event call,,,! : %o', event.data);
  }

}

//파일을 arrayBuffer로 변환해 chunck시켜 데이터채널로 전송하는 함수
function sendData(dataChannel) {
    console.log('sendData 호출....!');

    let offset = 0; //전송된 파일사이즈 표기변수
    let maxChunkSize = 16384; //16KB
    //progressbar.value = 0; //프로그래스바 초기화
    
    console.log(dataChannel.bufferedAmountLowThreshold);

    //1.Blob 메서드 arrayBuffer 를 사용하여 파일을 배열 버퍼로 변경
    file.arrayBuffer().then(buffer => {

        const send = () => {
            //buffer사이즈만큼 반복문을 돈다
            while (buffer.byteLength) {

                
                //로그만 찍어봤을땐, 버퍼가 가득차면 비우고, 다시 전송하는 느낌?
                // console.log('dataChannel.bufferedAmountLowThreshold : %o', dataChannel.bufferedAmountLowThreshold);
                // console.log('dataChannel.bufferedAmount : %o', dataChannel.bufferedAmount);

                /**
                 * bufferedAmount: 데이터 채널을 통해 전송되기 위해 현재 큐에 있는 데이터의 바이트 수를 반환
                 * bufferedAmountLowThreshold: "낮음"으로 간주되는 버퍼링된 나가는 데이터의 바이트 수를 지정합니다. 기본값은 0입니다.
                 * 
                 * dataChhannel.BufferedAmount() get's full 이슈를 해결하기 위한 코드
                 * dataChannel bufferedAmount가 16Kib 아래로 감소할 때 수신 대기할 콜백 수신기를 구현
                */
                if (dataChannel.bufferedAmount > dataChannel.bufferedAmountLowThreshold) {
                    //현재 아웃바운드 데이터 전송 버퍼에 있는 바이트 수가 에 지정된 임계값 아래로 떨어지면 발생되는 이벤트
                    dataChannel.onbufferedamountlow = () => {
                        console.log('onbufferedamountlow 이벤트 발생!!!!');
                        dataChannel.onbufferedamountlow = null;
                        send();
                    };
                    return;
                }
                
                //2.전체 파일이 올 때까지 청크를 하나씩 보낸다.
                const chunk = buffer.slice(0, maxChunkSize);
                console.log('chunk생성...!');

                //3. 전송된 size만큼 자른뒤 배열 업데이트.
                buffer = buffer.slice(maxChunkSize, buffer.byteLength);

                console.log('datachannel file send...!');

                //chunk시킨 데이터를 send
                dataChannel.send(chunk);

                offset += maxChunkSize;
                //데이터를 얼만큼 보냈는지 누적된 size값을 로그로남긴다
                //console.log('Sent ' + offset + ' bytes.');

                //전송된 파일사이즈 양을 프로그레스바에 표시한다.
                //progressbar.value = offset >= file.size ? file.size : offset;

                //프로그래스바 파일전송상태를 %로 나타냄
                //progressLabel.innerHTML = offset >= file.size ? 'File sent' : (offset/file.size*100).toFixed(1) + '%';
            }
        };

        send();

    })

    //console.log(`File is ${[file.name, file.size, file.type, file.lastModified].join(' ')}`);
    //fileReader = new FileReader();

}




//파일전송 성공/실패 팝업 노출 
function showAlert(state) {
  const alert = document.querySelector(`.alert.alert-${state}`);
  alert.style.display = 'block';
  setTimeout(() => {
    alert.style.display = 'none';
  }, 2500);
}


/**
 * 로컬에서 다운로드DOM을 만들기 위해 함수생성
 */
function createFlieDownloadDom(filename, isOnePeer = false) {
  try {
     //"파일 끝" 메시지를 수신하고 청크를 큰 배열 버퍼에 함께 넣는 코드
    const arrayBuffer = localReceivedBuffers.reduce((acc, arrayBuffer) => {
      const tmp = new Uint8Array(acc.byteLength + arrayBuffer.byteLength);
      tmp.set(new Uint8Array(acc), 0);
      tmp.set(new Uint8Array(arrayBuffer), acc.byteLength);
      return tmp;
    }, new Uint8Array());

    //배열 버퍼를 Blob으로 변환
    const blob = new Blob([arrayBuffer]);
    console.log('blob : %o', blob);

    //파일을 다운로드하는 DOM UI를 그리는 함수
    downloadFile(blob, filename, nickname, {mine: true});

    //arrayBuffer data 초기화
    localReceivedBuffers = [];

  } catch (error) {
    console.error(error);
    closeDialog();
    showAlert('fail');
  }

  if (isOnePeer) {
    console.log('피어가 한명일때 때 popup close....!');
    closeDialog();

    //파일전송 성공 다이알로그 보였다가 사라지게
    showAlert('success');
  }
  

}


//파일전송버튼 클릭 후 파일을 다른파일에 전송하는 처리
document.getElementById('ok-button').addEventListener('click', () => {
  shareFile();
});

const closeDialog = () => {
  document.getElementById('select-file-input').value = '';
  document.getElementById('select-file-dialog').style.display = 'none';
};


//샘플 파일다운로드 
const downloadFile2 = (blob, fileName) => {
    const a = document.createElement('a');
    const url = window.URL.createObjectURL(blob);
    a.href = url;
    a.download = fileName;
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove()
};


//파일다운로드 URL 생성
const downloadFile = (blob, fileName, nickname, mine = false) => {
  
  //띄어쓰기 포함해서 파일전송하는 방법
  const downloadDom = `download="${fileName}"`;
  console.log('downloadDom: %o', downloadDom);


  //const a = document.createElement('a');
  //주어진 객체를 가리키는 URL을 DOMString 으로 반환한다.

  const url = window.URL.createObjectURL(blob);

  const chatBox = document.querySelector('.chat__box');
  const li = document.createElement("li");
  const time = moment(new Date()).format("h:mm A") //현재시간 오전/오후


  // 채팅화면처럼 다운로드 화면 그리기
  let myNickname = '';
  if(mine){
    myNickname = `${nickname}[나]`;
  }

  //미로그인의 경우 디폴트 이미지가 노출되도록 처리
  let profileImg = '';
  if(isLogin) {
    profileImg = '';
  } else {
    profileImg = `${window.location.origin}/public/image/default_user.png`;
  }

  //일반 채팅메세지UI 그리기
  li.innerHTML = `<li>
      <span class="profile">
        <img class="image" src=${profileImg} alt="any" srcset="">
      </span>
      <div class="message__info">
        <span class="user ${mine ? 'mine': ''}">${mine? myNickname : nickname}</span>
        <span class="message file">
        <i class="fas fa-file fa-2x"></i>
          <a class="file__down" ${downloadDom}
            href=${url}>${fileName}</a>
          <span class="file__down-info">-클릭후 파일열기</span>
          <progress id="progress" value="0"></progress>&nbsp;&nbsp;<label id="progressLabel"></label>
        </span>

        <span class="time">${time}</span>
      </div>
    </li>`;
  console.log('li값 확인 : %o', li);

  chatBox.appendChild(li);
  //a.click();
  
  //객체URL을 해제한다
  //window.URL.revokeObjectURL(url);
  //a.remove()
};
