/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.call.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.activities.call.viewmodels.CallsViewModel
import org.linphone.activities.call.viewmodels.ControlsViewModel
import org.linphone.activities.call.viewmodels.SharedCallViewModel
import org.linphone.activities.main.MainActivity
import org.linphone.activities.main.viewmodels.DialogViewModel
import org.linphone.core.Call
import org.linphone.databinding.CallControlsFragmentBinding
import org.linphone.utils.AppUtils
import org.linphone.utils.DialogUtils
import org.linphone.utils.Event

class ControlsFragment : Fragment() {
    private lateinit var binding: CallControlsFragmentBinding
    private lateinit var callsViewModel: CallsViewModel
    private lateinit var controlsViewModel: ControlsViewModel
    private lateinit var sharedViewModel: SharedCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallControlsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.lifecycleOwner = this

        sharedViewModel = activity?.run {
            ViewModelProvider(this).get(SharedCallViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)
        binding.viewModel = callsViewModel

        controlsViewModel = ViewModelProvider(this).get(ControlsViewModel::class.java)
        binding.controlsViewModel = controlsViewModel

        callsViewModel.currentCallViewModel.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.activeCallTimer.base =
                    SystemClock.elapsedRealtime() - (1000 * it.call.duration) // Linphone timestamps are in seconds
                binding.activeCallTimer.start()
            }
        })

        callsViewModel.noMoreCallEvent.observe(viewLifecycleOwner, Observer {
            it.consume {
                activity?.finish()
            }
        })

        callsViewModel.callUpdateEvent.observe(viewLifecycleOwner, Observer {
            it.consume { call ->
                showCallUpdateDialog(call)
            }
        })

        controlsViewModel.chatClickedEvent.observe(viewLifecycleOwner, Observer {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra("Chat", true)
                startActivity(intent)
            }
        })

        controlsViewModel.addCallClickedEvent.observe(viewLifecycleOwner, Observer {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra("Dialer", true)
                intent.putExtra("Transfer", false)
                startActivity(intent)
            }
        })

        controlsViewModel.transferCallClickedEvent.observe(viewLifecycleOwner, Observer {
            it.consume {
                val intent = Intent()
                intent.setClass(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra("Dialer", true)
                intent.putExtra("Transfer", true)
                startActivity(intent)
            }
        })

        controlsViewModel.somethingClickedEvent.observe(viewLifecycleOwner, Observer {
            it.consume {
                sharedViewModel.resetHiddenInterfaceTimerInVideoCallEvent.value = Event(true)
            }
        })
    }

    private fun showCallUpdateDialog(call: Call) {
        val viewModel = DialogViewModel(AppUtils.getString(R.string.call_video_update_requested_dialog))
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), viewModel)

        viewModel.showCancelButton({
            callsViewModel.answerCallUpdateRequest(call, false)
            dialog.dismiss()
        }, getString(R.string.dialog_decline))

        viewModel.showOkButton({
            callsViewModel.answerCallUpdateRequest(call, true)
            dialog.dismiss()
        }, getString(R.string.dialog_accept))

        dialog.show()
    }
}
