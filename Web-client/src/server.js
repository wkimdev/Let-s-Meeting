import express from "express";
import SocketIO from "socket.io";
import http from "http";
let bodyParser = require('body-parser');
//로그인정보를 session에 저장하기 위해 사용하는 library
const session = require('express-session');
const fs = require('fs');

const app = express();

const PORT = process.env.PORT || 3004;

const MAXIMUM = 5;

let meetingNum = null;
let username = null;
let isReject = false; //회의방 입장제한 구분값

//회의준비화면에서 넘어오는 파라미터값
let fromPrepare = false;
let hostId = null;
let meetingId = null;
let isScreenShare = false; //화면공유상태 구분값


//user가 볼수있는 폴더경로를 따로 지정
app.use("/public", express.static(__dirname + "/public"));
//body-parser 설정
app.use(bodyParser.urlencoded({extended:true})); // request 객체의 body에 대한 url encoding의 확장을 할 수 있도록 하는 설정
app.use(bodyParser.json()); //request body에 오는 데이터를 json 형식으로 변환

//session 사용 등록
//store: 세션이 데이터를 저장하는 곳. default값은 Memory Store. 메모리는 서버나 클라이언트를 껐다 키면 사라지는 휘발성
app.use(session({
  HttpOnly: true, // 사용자가 자바스크립트를 통해서 세션을 사용할 수 없도록 강제
  secret: 'keyboard cat',  //세션을 암호화 해줌
  resave: false, //세션을 항상 저장할지 여부를 정하는 값. (false 권장)
  saveUninitialized: true, //초기화되지 않은채 스토어에 저장되는 세션
  //store: new FileStore() 
}));


let muted = false;
let cameraOff = false;
let maximumFromMobile = 2; //mobile에서 전달된 최대회의참가자수
let nickname = null;
let userEmail = null;

//웹템플릿 렌더링
//홈화면
app.get("/", (req, res) => {
  res.sendFile(__dirname + "/index.html");
});

//이용약관화면 이동
app.get("/rules/service", (req, res) => {
  res.sendFile(__dirname + "/rules_service.html");
});

//개인정보처리방침 이동
app.get("/rules/privacy", (req, res) => {
  res.sendFile(__dirname + "/rules_privacy.html");
});

//개인정보처리방침 내용
app.get('/content/privacy', (req, res) => {
  res.sendFile(__dirname + "/privacy_content.html");
})

//서비스이용약관 내용
app.get('/content/service', (req, res) => {
  res.sendFile(__dirname + "/service_content.html");
})

//모바일템플릿 렌더링
app.get("/mobile", (req, res) => {
  meetingNum = req.query.num;
  muted = req.query.muted;
  cameraOff = req.query.cameraOff;
  nickname = req.query.nickname;
  maximumFromMobile = req.query.maximum;
  userEmail = req.query.userEmail;
  hostId = req.query.hostId;
  res.sendFile(__dirname + "/index_mobile.html")
});

//로그인여부 확인 후 로그인화면 이동 처리
app.get('/login', (req, res) => {
  res.sendFile(__dirname + "/login.html");
})

//session에 로그인여부 저장
app.get('/login/state', (req, res) =>{
  //서버에 session값을 로그인성공 상태로 업데이트 요청
  req.session.logined = true;
  req.session.email = req.query.email;
  req.session.save(); //session값을 저장
  res.redirect('/');
})

//session에 담긴 이메일 정보 응답
app.get('/login/session', (req, res) => {
  console.log('login session api call...logined:  '+ req.session.logined);
  console.log('login session api call...session : '+ req.session.email);
  //return req.session.email;
  res.json({
    email: req.session.email
  });
})

//로그아웃 처리
app.get('/logout', (req, res) => {
  //session에 로그인여부 삭제
  req.session.destroy();
  res.redirect('/');
})


//회의화면
/**
 * @TODO 
 * 1) URL치면 그냥 회의 입장됨. 입장제한 시켜야함
 *  -> URL을 유니크하게 만들어서 제공해야할 듯 
 * 구글밋처럼 (https://meet.google.com/tdw-wgvr-qfe, )
 * 2) 회의중에 나갔을 경우 같은 URL로 회의준비화면으로 이동시켜야함 
 */
app.get("/meeting", (req, res) => {
  //검증을 거치고 접근해야 한다.
  res.sendFile(__dirname + "/meeting.html");
});

//회의 종료후 화면
app.get("/finish", (req, res) => {
  res.sendFile(__dirname + "/finish.html");
});

//회의 나간후 화면
app.get("/leave", (req, res) => {
  res.sendFile(__dirname + "/leave.html");
});

//회의준비화면
app.get('/prepare', (req, res) => {
  res.sendFile(__dirname + "/prepare.html");
})

//원래는 페이지를 이동시키는처리를 하려고 했으나 잘 되지 않아
//변수값만 할당하도록 처리함.
app.get("/meeting/:meetingNum/:username", (req, res) => {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            

  meetingNum = req.params.meetingNum;
  username = req.params.username;
  res.json({
    statusCode: '200',
    message: '회의방입장 정상 처리'
  });
});

app.get('/meeting/auth', (req, res) => {
  res.sendFile(__dirname + '/meeting_auth.html');
})

app.get("/:meetingNum/auth", (req, res) => {

  const meetingNum = req.params.meetingNum;
  /**
   * @TODO
   *  회의번호검증
   *  임시로 테스트ID로 검증
   */
  if (meetingNum !== '123') { //회의코드 검증실패
    //회의코드 확인화면 이동 
    res.sendFile(__dirname + "/meeting_auth.html");
  } else {//회의코드 검증 성공 
    //회의준비화면으로 이동 
    res.sendFile(__dirname + "/prepare.html");
  }
})

//호스트에 의해 회의가 아직 열리지 않았을 경우 페이지
app.get('/notopen', (req, res) => {
  res.sendFile(__dirname + '/notopen.html');
})

//회의인원수 제한으로 인한 참가제한 화면
app.get('/reject', (req, res) => {
  res.sendFile(__dirname + '/reject.html');
})

//화면공유중인지 여부 체크
app.get('/reject', (req, res) => {
  res.sendFile(__dirname + '/reject.html');
})


//회의준비화면에서 회의화면으로 진입할때만 호출하는 API
app.post('/meeting/prepare/info', (req, res) => {
  console.log('###### /meeting/prepare/info API call...!!!!');

  //회의준비 화면에서 입장했는지 구분하는 값
  fromPrepare = req.body.fromPrepare;
  console.log('fromPrepare :%o', fromPrepare);
  //호스트ID값 셋팅
  hostId = req.body.hostId;
  console.log('hostId :%o', hostId);

  meetingId = req.body.meetingId;
  console.log('meetingId :%o', meetingId);
  
  res.json({
    statusCode: '200',
    message: '회의방입장 정상 처리'
  });
})

//미디어트랙 정보 저장
//@TODO: 미디어트랙정보를 저장하는 방법?
app.post('/meeting/screen/share', (req, res) => {
  
  //1.값을 제대로 전달받는지 확인
  //값이 전달이 안됨
  //2.express에서 사용할 수 있는 임시저장소
  console.log('req.body값 확인 : %o', req.body);
})

//"/"경로 외에 다른 경로로 진입했을 때 리다이렉트
//app.get("/*", (req, res) => res.redirect("/"));

const httpServer = http.createServer(app);
const wsServer = SocketIO(httpServer);


//회의방 정보를 갖고 있는 배열 선언
let roomObjArr = [
  // {
  //   roomName,
  //   currentNum,
  //   users: [
  //     {
  //       socketId,
  //       nickname,
  //     },
  //   ],
  // },
];



//socket.io 연결후 이벤트 처린
wsServer.on("connection", (socket) => {
  //방번호, 닉네임 초기화
  let myRoomName = null;
  let myNickname = null;
  console.log('connection started...!');

  socket.on('request_mobile_meeting_info', () => {

    socket.join(meetingNum);
    //음소거여부, 카메라꺼짐 여부, 닉네임
    socket.emit("response_mobile_meeting_info", 
      meetingNum, muted, cameraOff, nickname, maximumFromMobile,
      userEmail, hostId);
  })
  
  //클라이언트에게 방에 입장한 사용자 정보를 전달해주기 위한 소켓 이벤트
  socket.on('request_meeting_info', () => {

    //소켓을 주어진 방이나 방 목록에 추가함으로서 
    //해당방에 입장했을 경우에만 소켓 이벤트 발생되도록 처리
    //join_room 이벤트를 Emit한 클라이언트에게만 이벤트 발생
    socket.join(meetingNum);
    socket.emit("send_meeting_info", hostId, fromPrepare, meetingId, socket.id, isScreenShare);

    //회의준비화면으로 부터 들어왔다는 구분값 초기화
    fromPrepare = false;
  })

  //회의참여 소켓이벤트 수신
  socket.on("join_room", (roomName, nickname, maximum) => {

    myRoomName = roomName;
    myNickname = nickname;

    console.log('### join_room event 발생!!! server.js maximum : %o', maximum);
    console.log(`##### join_room: 회의번호 : ${myRoomName}, 유저명 : ${myNickname} ##### `);

    let isRoomExist = false;
    let targetRoomObj = null;

    // forEach를 사용하지 않는 이유: callback함수를 사용하기 때문에 return이 효용없음.
    for (let i = 0; i < roomObjArr.length; ++i) {
      if (roomObjArr[i].myRoomName === myRoomName) {
        console.log(`기존 방에 입장, 방번호: ${myRoomName}`);
        // Reject join the room
        console.log('(roomObjArr[i].currentNum 값 확인: %o', roomObjArr[i].currentNum);
        
        if (roomObjArr[i].currentNum >= maximum) {
          isReject = true;
          socket.emit("reject_join");
          return;
        }

        isRoomExist = true;
        isReject = false;
        targetRoomObj = roomObjArr[i];
        break;
      }
    }

    //최초 회의룸 입장시 룸객체 생성
    if (!isRoomExist) {
      targetRoomObj = {
        myRoomName,
        currentNum: 0,
        users: [],
      };
      roomObjArr.push(targetRoomObj);
    }
        
    //룸에 join 시킨다
    targetRoomObj.users.push({
      socketId: socket.id,
      myNickname,
    });
    ++targetRoomObj.currentNum;

    console.log('###### 서버에서 전달하는 user객체 : %o', targetRoomObj);

    //소켓을 주어진 방이나 방 목록에 추가함으로서 
    //해당방에 입장했을 경우에만 소켓 이벤트 발생되도록 처리
    //join_room 이벤트를 Emit한 클라이언트에게만 이벤트 발생
    socket.join(myRoomName);
    socket.emit("welcome", targetRoomObj.users);
  });

  //offer 이벤트를 받아, offer객체와 roomName을 받고, 해당 룸에 offer 이벤트를 보낸다
  //isScreenShare: 화면공유중 상태값
  socket.on("offer", (offer, remoteSocketId, localNickname) => {
    //socket.to: 이벤트가 주어진 소켓ID(remoteSocketId)에 클라이언트에게만 브로드캐스트되도록 함
    //상대방에게 전달하는 offer, 내 SocketId, 내 닉네임
    socket.to(remoteSocketId).emit("offer", offer, socket.id, localNickname, isScreenShare);
  });

  //answer 이벤트를 받아, 해당 roomName에 answer 정보를 보낸다.
  socket.on('answer', (answer, remoteSocketId) => {
    //상대방에게 전달하는 answer, 내 socketId
    socket.to(remoteSocketId).emit('answer', answer, socket.id);
  })

  //브라우저들이 각각 ice candidate를 주고받는 이벤트처리
  socket.on("ice", (ice, remoteSocketId) => {
    //상대방에게 내 socketId와 ice candidate data를 전달한다.
    socket.to(remoteSocketId).emit("ice", ice, socket.id);
  })

  //chat 이벤트를 받아, 다른 피어에 채팅내용을전달한다
  socket.on("chat", (message, roomName, remoteNickname, time) => {
    console.log('send chat event from server...!');
    socket.to(roomName).emit("chat", message, remoteNickname, time);
  });

  let screenShareStatus = []; //화면공유 상태값 처리
  const screenShareObj = {
    socketId: '',
    isSharer: false
  };
  let isSharerLeave = false; //화면공유자가 나간건지 판별하는 구분값
  
  /**
   * remoteSocketId - 이벤트를 받을 소켓ID
   * changedSocketId - 화면공유를한 유저의 소켓ID
   * remoteNickname - 화면공유를한 유저의 닉네임
   * */
  socket.on('screen_share', (remoteSocketId, changedSocketId, remoteNickname) => {
    isScreenShare = true; //화면공유중 상태 변경
    
    //화면상태객체에 누가 화면을 공유중인지 값을 넣는다.
    screenShareObj.socketId = changedSocketId;
    screenShareObj.isSharer = true;
    screenShareStatus.push(screenShareObj);
    
    //배열에서 중복값 제거 
    screenShareStatus = screenShareStatus.filter((v, i) => screenShareStatus.indexOf(v) === i);
    
    console.log('!############## 화면 공유후 screenShareStatus 상태값 확인: %o', screenShareStatus);
    socket.to(remoteSocketId).emit('screen_share', changedSocketId, remoteNickname);
  })

  socket.on('screen_share_stop', (remoteSocketId, changedSocketId) => {
    isScreenShare = false; //화면 미공유중 상태 변경
    
    //화면상태객체에 누가 화면 공유를 취소했는지 값을 넣는다.
    screenShareStatus.forEach(item => {
      if (item.socketId === changedSocketId) {
        item.isSharer = false;
      }
    })
    
    console.log('!############## 공유취소 이후의 screenShareStatus 상태값 확인: %o', screenShareStatus);
    socket.to(remoteSocketId).emit('screen_share_stop', changedSocketId);
  })

  //화면공유자가 회의를 나갔을때 처리
  socket.on("finish_screen_share", () => {
    //화면공유자가 회의를 나갔기 때문에 공유화면을 종료하는 노티
    socket.to(myRoomName).emit("finish_screen_share", isScreenShare);
  })

  //호스트에 의해 회의가 종료되었을때 처리
  // ###### 호출이 안되는 중!!!
  socket.on('finish_meeting_by_host', () => {
    console.log("##### finish_meeting_by_host call...!!!");

    //호스트에 의해 회의가 종료되었다는 노티
    socket.to(myRoomName).emit("noti_finish_meeting", socket.id, myNickname);
  })

  //let sharerNickname = ''; //화면공유자 닉네임
  //화면공유중인 소켓ID값 요청
  // socket.on('request_share_screen', (socketId) => {
  //   console.log('########### 1. request_share_screen 요청...! o%', screenShareStatus);
  //   //지금 회의를 나가는 유저가 화면을 공유중이었는지를 확인
  //   screenShareStatus.forEach(item => {
  //     if (item.isSharer == true) { //화면공유상태
  //       if (item.socketId === socket.id) {
  //         //화면공유중인 user nicknname 찾기
  //         for (let i = 0; i < roomObjArr.length; ++i) {
  //           if (roomObjArr[i].myRoomName === myRoomName) {
  //             const roomObj = roomObjArr[i]; 
      
  //             roomObj.users.forEach(user => {
  //               if(user.socketId === item.socketId) {
  //                 sharerNickname = user.nickname;
  //               }
  //             })
  //           }
  //         }
  //         console.log('########### 1. response_sharerNickname 응답...! o%', sharerNickname);
  //         socket.to(socketId).emit("response_sharerNickname", sharerNickname);
  //       }
  //     }
  //   })
  // })

  //소켓 연결 끊겼을 경우 이벤트처리
  socket.on("disconnecting", () => {
    console.log(`##### socket연결 끊겼을 경우 이벤트 호출 확인....!!! :${socket.id}, ${myNickname}`);

    // isSharerLeave - 화면공유자가 나가는건지 구분값
    // 떠난 사용자의 소켓ID를 알 수 있음. 
    socket.join(myRoomName);
    if(!isReject) {
      
      //지금 회의를 나가는 유저가 화면을 공유중이었는지를 확인
      screenShareStatus.forEach(item => {
        if (item.isSharer == true) { //화면공유상태
          if (item.socketId === socket.id) { //나가는 유저랑 화면공유자가 같으면
            isSharerLeave = true;
            isScreenShare = false;
          }
        }
      })
      console.log('########### isSharerLeave 값 확인 : %o', isSharerLeave);
      console.log('########### isScreenShare 값 확인 : %o', isScreenShare);

      //socket.id -> 소켓연결이 끊긴 소켓ID
      //isSharerLeave -> 화면공유자가 회의방을 나갔는지 구분값
      socket.to(myRoomName).emit("leave_room", socket.id, myNickname, isScreenShare, isSharerLeave);
      //isScreenShare = false; //화면공유상태 초기화
    }

    //초기화
    isReject = false;

    let isRoomEmpty = false;
    //방정보 배열중에 떠난 유저정보 삭제
    for (let i = 0; i < roomObjArr.length; ++i) {
      console.log('1. roomObjArr 값 확인 : %o', roomObjArr);

      if (roomObjArr[i].myRoomName === myRoomName) {
        //연결이 끊긴 소켓아이디를 유저객체에서 제외시킴
        const newUsers = roomObjArr[i].users.filter(
          (user) => user.socketId != socket.id
        );
        console.log('2. newUsers 값 확인 : %o', newUsers);

        roomObjArr[i].users = newUsers;

        //업데이트된 배열의 길이만큼 현재 참여자갯수 업데이트
        roomObjArr[i].currentNum = newUsers.length;
        //--roomObjArr[i].currentNum;

        if (roomObjArr[i].currentNum == 0) {
          isRoomEmpty = true;
        }
      }
    }

    // Delete room
    if (isRoomEmpty) {
      const newRoomObjArr = roomObjArr.filter(
        (roomObj) => roomObj.currentNum != 0
      );
      roomObjArr = newRoomObjArr;
    }
  });

});


const handleListen = () =>
  console.log(`Listening on http://localhost:${PORT}`);
  
httpServer.listen(PORT, handleListen);
