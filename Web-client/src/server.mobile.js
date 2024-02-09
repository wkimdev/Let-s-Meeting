import express from "express";
import SocketIO from "socket.io";
import http from "http";

// ES modules
import { io } from "socket.io-client";
import { log } from "console";
const socket = io();

const app = express();

const PORT = process.env.PORT || 3004;

const MAXIMUM = 4;

const meetingNum = null;
const username = null;

//user가 볼수있는 폴더경로를 따로 지정
app.use("/public", express.static(__dirname + "/public"));

//웹템플릿 렌더링
//홈화면
app.get("/", (req, res) => {
  res.sendFile(__dirname + "/index.html");
});
//로그인화면
app.get('/login', (req, res) => {
  res.sendFile(__dirname + "/login.html");
})
//회의화면
app.get("/meeting", (req, res) => {
  res.sendFile(__dirname + "/meeting.html");
});


app.get("/meeting/:meetingNum/:nickname", (req, res) => {

  //방에 입장했다는 이벤트를 emit하고, 'welcome'이벤트를 받을 준비를 하게 된다.
  meetingNum = req.params.meetingNum;
  username = req.params.nickname;
  //이 값들을 소켓으로 곧바로 전달할 수 있는 방법???????
  // console.log(`meetingNum : ${meetingNum}`);
  // console.log(`nickname : ${nickname}`);
  //socket.emit('join_room', meetingNum, nickname);

  res.sendFile(__dirname + "/meeting.html");

});

//모바일템플릿 렌더링
app.get("/mobile", (req, res) => res.sendFile(__dirname + "/index_mobile.html"));

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

  socket.on("join_room", (roomName, nickname) => {

    myRoomName = roomName;
    myNickname = nickname;

    console.log(`join_room 이벤트발생 !, roomName: ${roomName}, nickname : ${nickname},
      socketId: ${socket.id}`);
    
    let isRoomExist = false;
    let targetRoomObj = null;

    // forEach를 사용하지 않는 이유: callback함수를 사용하기 때문에 return이 효용없음.
    for (let i = 0; i < roomObjArr.length; ++i) {
      if (roomObjArr[i].roomName === roomName) {
        console.log(`기존 방에 입장, 방번호: ${roomName}`);
        // Reject join the room
        if (roomObjArr[i].currentNum >= MAXIMUM) {
          socket.emit("reject_join");
          return;
        }

        isRoomExist = true;
        targetRoomObj = roomObjArr[i];
        break;
      }
    }

    //최초 회의룸 입장시 룸객체 생성
    if (!isRoomExist) {
      targetRoomObj = {
        roomName,
        currentNum: 0,
        users: [],
      };
      roomObjArr.push(targetRoomObj);
    }
        
    //룸에 join 시킨다
    targetRoomObj.users.push({
      socketId: socket.id,
      nickname,
    });
    ++targetRoomObj.currentNum;

    //소켓을 주어진 방이나 방 목록에 추가함으로서 
    //해당방에 입장했을 경우에만 소켓 이벤트 발생되도록 처리
    //join_room 이벤트를 Emit한 클라이언트에게만 이벤트 발생
    socket.join(roomName);
    socket.emit("welcome", targetRoomObj.users);
  });

  //offer 이벤트를 받아, offer객체와 roomName을 받고, 해당 룸에 offer 이벤트를 보낸다
  socket.on("offer", (offer, remoteSocketId, localNickname) => {    
    //socket.to: 이벤트가 주어진 소켓ID(remoteSocketId)에 클라이언트에게만 브로드캐스트되도록 함
    //상대방에게 전달하는 offer, 내 SocketId, 내 닉네임
    socket.to(remoteSocketId).emit("offer", offer, socket.id, localNickname);
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

  //소켓 연결 끊겼을 경우 이벤트처리
  socket.on("disconnecting", () => {
    socket.to(myRoomName).emit("leave_room", socket.id, myNickname);

    let isRoomEmpty = false;
    //나가는 방정보 배열중에 떠난 유저정보 삭제
    for (let i = 0; i < roomObjArr.length; ++i) {
      if (roomObjArr[i].roomName === myRoomName) {
        const newUsers = roomObjArr[i].users.filter(
          (user) => user.socketId != socket.id
        );
        roomObjArr[i].users = newUsers;
        --roomObjArr[i].currentNum;

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
